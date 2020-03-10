package rocks.voss.musicsync.plugins.output.filesystem;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import rocks.voss.musicsync.api.SyncConnection;
import rocks.voss.musicsync.api.SyncOutputPlugin;
import rocks.voss.musicsync.api.SyncTrack;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

public class FilesystemOutputPlugin implements SyncOutputPlugin {
    final private Logger log = Logger.getLogger(this.getClass().getName());
    private String directory;

    @Override
    public String helpScreen() {
        return "--filesystem-directory DIRECTORY\n\t\tSet the output path for the files\n";
    }

    @Override
    public void uploadTracks(SyncConnection connection, List<SyncTrack> syncTracks) {
        String outputPath = getOutputPath(connection);

        for (SyncTrack syncTrack : syncTracks) {
            log.info("Copying: " + getFilename(syncTrack));
            Runtime rt = Runtime.getRuntime();
            StringBuilder command = new StringBuilder();
            command.append("cp ")
                    .append("\"")
                    .append(syncTrack.getCacheLocation())
                    .append("\" \"")
                    .append(outputPath)
                    .append("/")
                    .append(getFilename(syncTrack))
                    .append("\"");

            String[] commands = {"/bin/bash", "-c", command.toString()};
            log.debug("Executing: " + command.toString());
            try {
                rt.exec(commands).waitFor();
            } catch (IOException e) {
                log.error("IOException", e);
            } catch (InterruptedException e) {
                log.error("InterruptedException", e);
            }
            log.debug("Execution done");
        }

    }

    @Override
    public boolean isTrackUploaded(SyncConnection connection, SyncTrack syncTrack) {
        String outputPath = getOutputPath(connection);

        File dir = new File(outputPath);
        File[] files = dir.listFiles((directory, dirFile) -> StringUtils.endsWith(dirFile, getFilename(syncTrack)));
        if (files != null && files.length > 0) {
            return true;
        }
        return false;
    }

    @Override
    public void orderTracks(SyncConnection connection, List<SyncTrack> syncTracks) {
        return;
    }

    @Override
    public void cleanUpTracks(SyncConnection connection, List<SyncTrack> syncTracks) {
        String outputPath = getOutputPath(connection);

        File dir = new File(outputPath);
        File[] files = dir.listFiles((directory, dirFile) -> {
            for (SyncTrack syncTrack : syncTracks) {
                if (StringUtils.equals(dirFile, getFilename(syncTrack))) {
                    return false;
                }
            }
            return true;
        });

        if (files != null) {
            for (File file : files) {
                file.delete();
            }
        }
    }

    @Override
    public void establishConnection() {
        return;
    }

    @Override
    public void init(Properties properties) throws Exception {
        directory = properties.getProperty("filesystem.directory");
    }

    @Override
    public boolean parseArguments(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (StringUtils.equals(arg, "--filesystem-directory")) {
                directory = args[++i];
                log.debug("--filesystem-directory: " + directory);
            }
        }
        log.debug("Parsing arguments is okay");
        return true;
    }

    @Override
    public String getSchema() {
        return "filesystem";
    }

    @Override
    public void closeConnection() {
        return;
    }

    private String getFilename(SyncTrack syncTrack) {
        String name = StringUtils.substring("00" + syncTrack.getTrackNumber(), 0, 3);
        name = name + "-" + syncTrack.getArtists()[0] + "-" + syncTrack.getName();
        return StringUtils.replace(name, " ", "_");
    }

    private String getOutputPath(SyncConnection connection) {
        String pairs[] = StringUtils.split(connection.getOutputUri(), ":");
        pairs[1] = StringUtils.trim(pairs[1]);
        if (StringUtils.isEmpty(pairs[1])) {
            return directory;
        } else {
            return pairs[1];
        }
    }
}
