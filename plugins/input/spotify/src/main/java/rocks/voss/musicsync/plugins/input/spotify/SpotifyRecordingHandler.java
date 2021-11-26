package rocks.voss.musicsync.plugins.input.spotify;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rocks.voss.musicsync.plugins.input.spotify.config.PluginConfiguration;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class SpotifyRecordingHandler {
    final private static Logger log = LogManager.getLogger(SpotifyRecordingHandler.class);

    public static boolean recordTrack(PluginConfiguration spotifyHandler, PlaylistTrack track, String filePath)
            throws IOException, InterruptedException {
        new File(spotifyHandler.getCachePath()).mkdirs();

        filePath = filePath.replace("//", "/");
        String filename = StringUtils.replace(filePath, spotifyHandler.getCachePath(), "");
        if (StringUtils.startsWith(filename, "/")) {
            filename = StringUtils.substring(filename, 1);
        }

        if (track.getTrack() == null) {
            log.debug("Track is not available on Spotify");
            return false;
        } else if (isFileValid(spotifyHandler, track, spotifyHandler.getCachePath(), filename)) {
            log.debug("File is valid: " + filename);
            return false;
        } else {
            log.debug("File is not valid: " + filename);
            downloadFile(spotifyHandler, track, filename);
            return true;
        }
    }

    private static void downloadFile(PluginConfiguration spotifyHandler, PlaylistTrack track, String filename) throws InterruptedException, IOException {
        log.info("Downloading: " + track.getTrack().getUri());
        Runtime rt = Runtime.getRuntime();
        StringBuilder command = new StringBuilder();
        command.append("stream_recorder.pl --uri '")
                .append(track.getTrack().getUri())
                .append("' --silent --format mp3 --outdir ")
                .append(spotifyHandler.getCachePath())
                .append(" --filename ")
                .append(filename);

        String[] commands = {"/bin/bash", "-c", command.toString()};
        log.debug("Executing: " + command.toString());
        rt.exec(commands).waitFor();
        log.debug("Execution done");
    }

    private static boolean isFileValid(PluginConfiguration spotifyHandler, PlaylistTrack track, String path, String filename) {
        log.debug("Checking: " + filename);

        File dir = new File(path);
        File[] files = dir.listFiles((directory, dirFile) -> StringUtils.equals(dirFile, filename));

        if (files != null && files.length == 1) {
            try {
                AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(files[0]);
                Map<?, ?> properties = fileFormat.properties();
                String key = "duration";
                Long microseconds = (Long) properties.get(key);
                int mili = (int) (microseconds / 1000);
                int delta = mili - track.getTrack().getDurationMs();

                log.debug("MP3 duration  : " + mili);
                log.debug("Track duration: " + track.getTrack().getDurationMs());
                log.debug("Delta duration: " + delta);

                if (delta > spotifyHandler.getTrackThreshold() || delta < -spotifyHandler.getTrackThreshold()) {
                    log.info("Delta is too big");
                    files[0].delete();
                    return false;
                }
            } catch (IOException e) {
                log.error("IOException in file: " + files[0].getAbsolutePath(), e);
                files[0].delete();
                return false;
            } catch (UnsupportedAudioFileException e) {
                log.error("UnsupportedAudioFileException in file: " + files[0].getAbsolutePath(), e);
                files[0].delete();
                return false;
            }
            log.info("File is okay: " + filename);
            return true;
        }
        log.info("File doesn't exists: " + filename);
        return false;
    }
}
