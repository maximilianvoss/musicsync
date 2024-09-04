package rocks.voss.musicsync.plugins.filesystemin;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import rocks.voss.jsonhelper.JSONHelper;
import rocks.voss.musicsync.api.SyncConnection;
import rocks.voss.musicsync.api.SyncInputPlugin;
import rocks.voss.musicsync.api.SyncTrack;
import rocks.voss.musicsync.plugins.filesystemin.config.PluginConfiguration;
import rocks.voss.musicsync.plugins.filesystemin.config.SyncConfiguration;

public class FilesystemInPlugin implements SyncInputPlugin {

    final private static Logger log = LogManager.getLogger(FilesystemInPlugin.class);
    private String directory;

    @Override
    public String helpScreen() {
        return StringUtils.EMPTY;
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
    public boolean parseArguments(String[] args) {
        return true;
    }

    @Override
    public String getSchema() {
        return "filesystemin";
    }

    @Override
    public void closeConnection() {
        return;
    }

    @Override
    public List<SyncTrack> getTracklist(SyncConnection connection) {
        File inputPath = new File(getInputPath(connection));
        log.info("Reading contents of input folder: " + inputPath.getAbsolutePath());
        int trackNumber = 0;
        File[] files = inputPath.listFiles();
        List<SyncTrack> tracks = new ArrayList<>();
        if (files != null) {
            Arrays.sort(files, (a, b) -> a.getName().compareTo(b.getName()));
            for (File file : files) {
                if (file.isDirectory()) {
                    continue;
                }
                // only supports mp3 files
                if (file.getName().endsWith(".mp3")) {
                    SyncTrack syncTrack = new FileSyncTrack(this, file, ++trackNumber);
                    if (syncTrack != null) {
                        log.info("Adding track: " + syncTrack.toString());
                        tracks.add(syncTrack);
                    }
                } else {
                    log.info("Skipping file: " + file.getAbsolutePath());
                }
            }
        }
        return tracks;
    }

    private String getInputPath(SyncConnection connection) {
        try {
            SyncConfiguration syncConfiguration = JSONHelper.createBean(SyncConfiguration.class,
                    connection.getInputConfig());
            if (syncConfiguration.getDirectory() != null) {
                return syncConfiguration.getDirectory();
            }
        } catch (IOException e) {
            log.error(e);
        }
        return directory;
    }
}
