package rocks.voss.musicsync.plugins.filesystemin;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rocks.voss.musicsync.api.SyncPlugin;
import rocks.voss.musicsync.api.SyncTrack;

public class FileSyncTrack implements SyncTrack {
    final private static Logger log = LogManager.getLogger(FileSyncTrack.class);

    private final String filePath;
    private final String fileName;
    private final int trackDuration;
    private final String uri;
    private final int trackNumber;
    private final String artist;
    private final String title;
    private final String album;
    private SyncPlugin plugin;

    public FileSyncTrack(SyncPlugin plugin, File file, int trackNumber) {
        this.plugin = plugin;
        this.fileName = file.getName();
        this.filePath = file.getAbsolutePath();
        this.uri = file.toURI().toString();
        this.artist = getArtist(file);
        this.title = getTitle(file);
        this.album = getAlbumName(file);
        this.trackNumber = trackNumber;
        this.trackDuration = (int) getDuration(file);
    }

    @Override
    public String toString() {
        return "FileSyncTrack [absolutePath=" + filePath + ", trackDuration=" + trackDuration + ", uri=" + uri
                + ", trackNumber=" + trackNumber + ", artist=" + artist + ", title=" + title + ", album=" + album + "]";
    }

    private String getInfo(File file, String info) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", "-c", "ffprobe -v error -show_entries "
                    + info + " -of default=noprint_wrappers=1:nokey=1 '" + file.getAbsolutePath() + "'");
            processBuilder.redirectErrorStream(true);
            log.debug("Executing: " + processBuilder.command().toString());
            Process process = processBuilder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String stdout = reader.readLine();
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                log.debug("Exit code: " + exitCode + ", Output: " + stdout);
                return stdout;

            } else {
                log.error("Execution failed '" + processBuilder.command().toString() + "': Exit code " + exitCode
                        + "; Output: " + stdout);
            }
        } catch (IOException | InterruptedException e) {
            log.error("Excecution failed", e);
        }
        return null;
    }

    private double getDuration(File file) {
        String info = getInfo(file, "format=duration");
        double duration = 0;
        if (info != null) {
            try {
                duration = Double.parseDouble(info) * 1000;
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return duration;
    }

    private String getAlbumName(File file) {
        String info = getInfo(file, "format_tags=album");
        if (info != null) {
            return info;
        }
        return "";
    }

    private String getTitle(File file) {
        String info = getInfo(file, "format_tags=title");
        if (info != null) {
            return info;
        }
        // return name without extension
        String name = file.getName();
        int extensionIndex = name.lastIndexOf(".");
        name = name.substring(0, extensionIndex);
        // remove leading digits and replace underscores
        name = name.replaceFirst("^\\d+_", "");
        name = name.replace('_', ' ').trim();
        return name;
    }

    private String getArtist(File file) {
        String info = getInfo(file, "format_tags=artist");
        if (info != null) {
            return info;
        }
        return "";
    }

    @Override
    public SyncPlugin getPlugin() {
        return this.plugin;
    }

    @Override
    public String getId() {
        // to be compatible with spotify plugin
        String name = StringUtils.leftPad(this.fileName, 16, "_");
        if (name.length() > 16) {
            name = name.substring(0, 16);
        }
        try {
            return new String(Base62Encoder.createInstance().encode(name.getBytes("UTF-8")), "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public String getUri() {
        return this.uri;
    }

    @Override
    public String[] getArtists() {
        if (StringUtils.isEmpty(this.artist)) {
            return new String[] { "Unknown" };
        }
        return new String[] { this.artist };
    }

    @Override
    public String getName() {
        return this.title;
    }

    @Override
    public int getTrackNumber() {
        return this.trackNumber;
    }

    @Override
    public int getTrackDuration() {
        return this.trackDuration;
    }

    @Override
    public String getAlbum() {
        return this.album;
    }

    @Override
    public String getPath() {
        return this.filePath;
    }

    @Override
    public boolean isFresh() {
        return false;
    }
}