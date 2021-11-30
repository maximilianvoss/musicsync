package rocks.voss.musicsync.application;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import rocks.voss.jsonhelper.JSONHelper;
import rocks.voss.musicsync.api.SyncConnection;
import rocks.voss.musicsync.api.SyncInputPlugin;
import rocks.voss.musicsync.api.SyncOutputPlugin;
import rocks.voss.musicsync.api.SyncTrack;
import rocks.voss.musicsync.application.config.Configuration;
import rocks.voss.musicsync.application.config.SyncConfiguration;
import rocks.voss.musicsync.application.impl.SyncConnectionImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class Application {
    final private static Logger log = LogManager.getLogger(Application.class);
    private static List<SyncConnection> connections;

    public static void main(String[] args) throws Exception {
        File file = new File("log4j2.xml");
        if (file != null) {
            LoggerContext context = (org.apache.logging.log4j.core.LoggerContext) LogManager.getContext(false);
            context.setConfigLocation(file.toURI());
        }

        InputStream jsonStream = new FileInputStream("musicsync.json");
        Configuration config = JSONHelper.createBean(Configuration.class, jsonStream);

        PluginLoader.loadPlugins();
        if (!PluginLoader.initPlugins(config, args)) {
            printHelp();
        }

        connections = getConnections(config);
        log.debug("Starting in daemon mode");
        while (true) {
            try {
                sync(config, connections);
                log.debug("Taking a nap for: " + config.getGeneral().getTimeout() * 1000 + " seconds.");
                Thread.sleep(config.getGeneral().getTimeout() * 1000);
                log.debug("Up again");
            } catch (Exception e) {
                log.error("Exception", e);
            }
        }
    }

    private static void sync(Configuration config, List<SyncConnection> connections) {
        for (SyncConnection connection : connections) {
            try {
                SyncInputPlugin inputPlugin = connection.getSyncInputPlugin();
                SyncOutputPlugin outputPlugin = connection.getSyncOutputPlugin();

                log.info("Working on connection: " + connection.getName());

                if (inputPlugin == null || outputPlugin == null) {
                    log.error("One is null\nInputPlugin: " + inputPlugin + ", outputPlugin: " + outputPlugin);
                }

                inputPlugin.establishConnection();
                outputPlugin.establishConnection();

                if (config.getGeneral().isBulk()) {
                    syncBulk(connection);
                } else {
                    syncItemized(connection);
                }

                inputPlugin.closeConnection();
                outputPlugin.closeConnection();
            } catch (Exception e) {
                log.error("Exception", e);
            }
        }
    }

    private static void syncItemized(SyncConnection connection) {
        SyncInputPlugin inputPlugin = connection.getSyncInputPlugin();
        SyncOutputPlugin outputPlugin = connection.getSyncOutputPlugin();

        List<SyncTrack> tracks = inputPlugin.getTracklist(connection);
        outputPlugin.cleanUpTracks(connection, tracks);
        for (SyncTrack track : tracks) {
            if (!outputPlugin.isTrackUploaded(connection, track)) {
                inputPlugin.downloadTrack(connection, track);
                outputPlugin.uploadTrack(connection, track);
            }
        }
        outputPlugin.orderTracks(connection, tracks);
    }

    private static void syncBulk(SyncConnection connection) {
        SyncInputPlugin inputPlugin = connection.getSyncInputPlugin();
        SyncOutputPlugin outputPlugin = connection.getSyncOutputPlugin();

        List<SyncTrack> tracks = inputPlugin.getTracklist(connection);
        List<SyncTrack> tracksToSync = new ArrayList<>();
        for (SyncTrack track : tracks) {
            if (!outputPlugin.isTrackUploaded(connection, track)) {
                tracksToSync.add(track);
            }
        }
        inputPlugin.downloadTracks(connection, tracksToSync);

        outputPlugin.cleanUpTracks(connection, tracks);
        outputPlugin.uploadTracks(connection, tracksToSync);
        outputPlugin.orderTracks(connection, tracks);
    }

    private static List<SyncConnection> getConnections(Configuration config) {
        List<SyncConnection> connections = new ArrayList<>();
        for (SyncConfiguration connectionWrapperBean : config.getConnections()) {
            connections.add(SyncConnectionImpl.createBy(connectionWrapperBean.getName(), connectionWrapperBean.getIn(), connectionWrapperBean.getOut()));
        }
        return connections;
    }

    private static void printHelp() {
        StringBuilder help = new StringBuilder("Usage: spotify-toniebox-sync.jar\n");
        PluginLoader.getHelpMessages(help);
        System.out.println(help);
    }
}
