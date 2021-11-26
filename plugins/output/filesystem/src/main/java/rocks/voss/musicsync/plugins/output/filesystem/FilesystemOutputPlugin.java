package rocks.voss.musicsync.plugins.output.filesystem;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rocks.voss.jsonhelper.JSONHelper;
import rocks.voss.musicsync.api.SyncConnection;
import rocks.voss.musicsync.api.SyncOutputPlugin;
import rocks.voss.musicsync.api.SyncTrack;
import rocks.voss.musicsync.plugins.output.filesystem.config.PluginConfiguration;
import rocks.voss.musicsync.plugins.output.filesystem.config.SyncConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FilesystemOutputPlugin implements SyncOutputPlugin {
    final private static Logger log = LogManager.getLogger(FilesystemOutputPlugin.class);
    private String directory;

    @Override
    public String helpScreen() {
        return "--filesystem-directory DIRECTORY\n\t\tSet the output path for the files\n";
    }

    @Override
    public void uploadTracks(SyncConnection connection, List<SyncTrack> syncTracks) {
        for (SyncTrack syncTrack : syncTracks) {
            uploadTrack(connection, syncTrack);
        }
    }

    @Override
    public void uploadTrack(SyncConnection connection, SyncTrack syncTrack) {
        try {
            String outputPath = getOutputPath(connection);
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

            rt.exec(commands).waitFor();
            log.debug("Execution done");
        } catch (Exception e) {
            log.error("Exception", e);
        }
    }

    @Override
    public boolean isTrackUploaded(SyncConnection connection, SyncTrack syncTrack) {
        try {
            String outputPath = getOutputPath(connection);

            File dir = new File(outputPath);
            File[] files = dir.listFiles((directory, dirFile) -> StringUtils.endsWith(dirFile, getFilename(syncTrack)));
            if (files != null && files.length > 0) {
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("Exception", e);
            return false;
        }
    }

    @Override
    public void orderTracks(SyncConnection connection, List<SyncTrack> syncTracks) {
        return;
    }

    @Override
    public void cleanUpTracks(SyncConnection connection, List<SyncTrack> syncTracks) {
        try {
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
        } catch (Exception e) {
            log.error("Exception", e);
        }
    }

    @Override
    public void establishConnection() {
        return;
    }

    @Override
    public void init(Object configuration) throws Exception {
        PluginConfiguration pluginConfiguration = JSONHelper.createBean(PluginConfiguration.class, configuration);
        directory = pluginConfiguration.getDirectory();
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
        try {
            SyncConfiguration syncConfiguration = JSONHelper.createBean(SyncConfiguration.class, connection.getOutputConfig());
            if (syncConfiguration.getDirectory() != null) {
                return syncConfiguration.getDirectory();
            }
        } catch (IOException e) {
            log.error(e);
        }
        return directory;
    }
}
