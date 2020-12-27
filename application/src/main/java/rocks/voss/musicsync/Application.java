package rocks.voss.musicsync;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import rocks.voss.musicsync.api.SyncConnection;
import rocks.voss.musicsync.api.SyncInputPlugin;
import rocks.voss.musicsync.api.SyncOutputPlugin;
import rocks.voss.musicsync.api.SyncTrack;
import rocks.voss.musicsync.impl.SyncConnectionImpl;

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

    private static List<SyncConnection> connections;
    private static Properties properties;
    private static boolean isDaemon = false;
    private static String argInputUri = null;
    private static String argOutputUri = null;

    public static void main(String[] args) throws Exception {
        properties = getProperties();
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
                    log.error("Exception", e);
                }
            }
        } else {
            log.debug("Starting in single mode");
            sync(connections);
        }
    }

    private static void sync(List<SyncConnection> connections) {
        for (SyncConnection connection : connections) {
            SyncInputPlugin inputPlugin = connection.getSyncInputPlugin();
            SyncOutputPlugin outputPlugin = connection.getSyncOutputPlugin();

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
        boolean hasOtherArguments = false;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (StringUtils.equals(arg, "--daemon")) {
                isDaemon = true;
            } else if (StringUtils.equals(arg, "--input")) {
                argInputUri = args[++i];
            } else if (StringUtils.equals(arg, "--output")) {
                argOutputUri = args[++i];
            } else {
                hasOtherArguments = true;
            }
        }

        if (isDaemon && StringUtils.isEmpty(argInputUri) && StringUtils.isEmpty(argOutputUri)) {
            log.debug("Daemon parameters okay");
            return true;
        } else if (!isDaemon && StringUtils.isNotEmpty(argInputUri) && StringUtils.isNotEmpty(argOutputUri)) {
            log.debug("No Daemon parameters okay");
            return true;
        } else if (!isDaemon && StringUtils.isEmpty(argInputUri) && StringUtils.isEmpty(argOutputUri) && hasOtherArguments) {
            log.debug("No input and output specified. Parameters will be passed to plugin parsers.");
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

    private static Properties getProperties() throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream;
        if (new File("./" + PROPERTIES_FILE).exists()) {
            stream = new FileInputStream("./" + PROPERTIES_FILE);
        } else {
            stream = loader.getResourceAsStream(PROPERTIES_FILE);
        }
        Properties properties = new Properties();
        properties.load(stream);
        if (stream != null) {
            stream.close();
        }
        return properties;
    }
}
