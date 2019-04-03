package rocks.voss.musicsync;

import org.reflections.Reflections;
import rocks.voss.musicsync.api.SyncConnection;
import rocks.voss.musicsync.api.SyncInputPlugin;
import rocks.voss.musicsync.api.SyncOutputPlugin;
import rocks.voss.musicsync.api.SyncPlugin;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class PluginLoader {
    private static Map<String, SyncInputPlugin> inputPlugins = new HashMap<>();
    private static Map<String, SyncOutputPlugin> outputPlugins = new HashMap<>();

    public static void loadPlugins() throws InstantiationException, IllegalAccessException {
        loadInputPlugins();
        loadOutputPlugins();
    }

    public static boolean initPlugins(Properties properties, String[] args) throws Exception {
        return PluginLoader.initPlugins(properties, (Collection<SyncPlugin>) (Collection<?>) inputPlugins.values(), args)
                && PluginLoader.initPlugins(properties, (Collection<SyncPlugin>) (Collection<?>) outputPlugins.values(), args);
    }

    public static String getHelpMessages(StringBuilder helpMessages) {
        helpMessages.append("\nInput Plugins:\n");
        for (SyncInputPlugin inputPlugin : inputPlugins.values()) {
            helpMessages.append(inputPlugin.helpScreen());
        }
        helpMessages.append("\nOutput Plugins:\n");
        for (SyncOutputPlugin outputPlugin : outputPlugins.values()) {
            helpMessages.append(outputPlugin.helpScreen());
        }
        return helpMessages.toString();
    }

    public static void registerConnections(List<SyncConnection> connections) {
        for (SyncConnection connection : connections) {
            connection.setSyncInputPlugin(inputPlugins.get(connection.getInputSchema()));
            connection.setSyncOutputPlugin(outputPlugins.get(connection.getOutputSchema()));
        }
    }

    private static boolean initPlugins(Properties properties, Collection<SyncPlugin> plugins, String[] args) throws Exception {
        for (SyncPlugin plugin : plugins) {
            plugin.init(properties);
            if (!plugin.parseArguments(args)) {
                return false;
            }
        }
        return true;
    }

    private static void loadInputPlugins() throws IllegalAccessException, InstantiationException {
        loadPlugins(inputPlugins, SyncInputPlugin.class);
    }

    private static void loadOutputPlugins() throws IllegalAccessException, InstantiationException {
        loadPlugins(outputPlugins, SyncOutputPlugin.class);
    }

    private static <T> void loadPlugins(Map<String, T> map, Class<T> clazz) throws IllegalAccessException, InstantiationException {
        Reflections reflections = new Reflections("");
        Set<Class<? extends T>> subTypes = reflections.getSubTypesOf(clazz);
        Iterator<Class<? extends T>> iterator = subTypes.iterator();
        while (iterator.hasNext()) {
            T instance = iterator.next().newInstance();
            map.put(((SyncPlugin) instance).getSchema(), instance);
        }
    }
}

