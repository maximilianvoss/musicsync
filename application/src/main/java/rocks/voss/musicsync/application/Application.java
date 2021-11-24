package rocks.voss.musicsync.application;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import rocks.voss.musicsync.api.SyncConnection;
import rocks.voss.musicsync.api.SyncInputPlugin;
import rocks.voss.musicsync.api.SyncOutputPlugin;
import rocks.voss.musicsync.api.SyncTrack;
import rocks.voss.musicsync.application.impl.SyncConnectionImpl;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Application {
    final private static Logger log = Logger.getLogger(Application.class.getName());
    final private static String PROPERTIES_FILE = "musicsync.properties";
    final private static String LOG4J_PROPERTIES_FILE = "log4j.properties";

    private static List<SyncConnection> connections;
    private static Properties properties;
    private static boolean isDaemon = false;
    private static String argInputUri = null;
    private static String argOutputUri = null;

    public static void main(String[] args) throws Exception {
        properties = getProperties("./" + PROPERTIES_FILE, PROPERTIES_FILE);
        updateLoggerConfig();
        PluginLoader.loadPlugins();

        if (!parseArguments(args) || !PluginLoader.initPlugins(properties, args)) {
            printHelp();
        }

        connections = getConnections(properties);
        if (isDaemon) {
            log.debug("Starting in daemon mode");
            while (true) {
                try {
                    sync(connections);
                    log.debug("Taking a nap");
                    Thread.sleep(60000);
                    log.debug("Up again");
                } catch (Exception e) {
                    log.error("Exception");
                }
            }
        } else {
            log.debug("Starting in single mode");
            sync(connections);
        }
    }

    private static void sync(List<SyncConnection> connections) {
        for (SyncConnection connection : connections) {
            log.debug("Syncing " + connection.getInputUri() + " to " + connection.getOutputUri());
            try {
                SyncInputPlugin inputPlugin = connection.getSyncInputPlugin();
                SyncOutputPlugin outputPlugin = connection.getSyncOutputPlugin();

                if (inputPlugin == null || outputPlugin == null) {
                    log.error("One is null\nInputPlugin: " + inputPlugin + ", outputPlugin: " + outputPlugin);
                }

                inputPlugin.establishConnection();
                outputPlugin.establishConnection();

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

                inputPlugin.closeConnection();
                outputPlugin.closeConnection();
            } catch (Exception e) {
                log.error("Exception", e);
            }
        }
    }

    private static List<SyncConnection> getConnections(Properties properties) {
        List<SyncConnection> connections = new ArrayList<>();
        if (isDaemon) {
            String mapping;
            int i = 0;
            do {
                mapping = properties.getProperty("mapping[" + i + "]");
                log.debug("Mapping from file: " + mapping);
                if (mapping != null) {
                    String[] pairs = mapping.split(";");
                    connections.add(SyncConnectionImpl.createByUris(pairs[0], pairs[1]));
                }
                i++;
            } while (mapping != null);
        }
        if (StringUtils.isNotBlank(argInputUri) && StringUtils.isNotBlank(argOutputUri)) {
            connections.add(SyncConnectionImpl.createByUris(argInputUri, argOutputUri));
        }
        return connections;
    }

    private static boolean parseArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (StringUtils.equals(arg, "--daemon")) {
                isDaemon = true;
            } else if (StringUtils.equals(arg, "--input")) {
                argInputUri = args[++i];
            } else if (StringUtils.equals(arg, "--output")) {
                argOutputUri = args[++i];
            }
        }

        if (isDaemon && StringUtils.isEmpty(argInputUri) && StringUtils.isEmpty(argOutputUri)) {
            log.debug("Daemon parameters okay");
            return true;
        } else if (!isDaemon && StringUtils.isNotEmpty(argInputUri) && StringUtils.isNotEmpty(argOutputUri)) {
            log.debug("No Daemon parameters okay");
            return true;
        } else {
            log.debug("Application parameter not okay");
            return false;
        }
    }

    private static void printHelp() {
        StringBuilder help = new StringBuilder("Usage: spotify-toniebox-sync.jar [--daemon | --input INPUT --output OUTPUT]\n");
        help.append("--daemon\n\t\tRun in daemon mode to sync periodically all lists in the properties file\n");
        help.append("--input INPUT\n\t\tDefine an input source for a one-time run\n");
        help.append("--output OUTPUT\n\t\tDefine an output destination for a one-time run\n");
        PluginLoader.getHelpMessages(help);
        System.out.println(help);
    }

    private static Properties getProperties(String file, String resourcePath) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream;
        if (new File(file).exists()) {
            stream = new FileInputStream(file);
        } else {
            stream = loader.getResourceAsStream(resourcePath);
        }
        Properties properties = new Properties();
        properties.load(stream);
        if (stream != null) {
            stream.close();
        }
        return properties;
    }

    private static void updateLoggerConfig() throws IOException {
        Properties props = getProperties("./" + LOG4J_PROPERTIES_FILE, LOG4J_PROPERTIES_FILE);
        LogManager.resetConfiguration();
        PropertyConfigurator.configure(props);
    }
}
