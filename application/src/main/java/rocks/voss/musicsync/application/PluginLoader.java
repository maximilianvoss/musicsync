package rocks.voss.musicsync.application;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import rocks.voss.musicsync.api.SyncInputPlugin;
import rocks.voss.musicsync.api.SyncOutputPlugin;
import rocks.voss.musicsync.api.SyncPlugin;
import rocks.voss.musicsync.application.config.Configuration;
import rocks.voss.musicsync.application.config.PluginConfiguration;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public class PluginLoader {
    @Getter
    private static Map<String, SyncInputPlugin> inputPlugins = new HashMap<>();
    @Getter
    private static Map<String, SyncOutputPlugin> outputPlugins = new HashMap<>();

    public static void loadPlugins() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        loadInputPlugins();
        loadOutputPlugins();
    }

    public static boolean initPlugins(Configuration config, String[] args) throws Exception {
        return PluginLoader.initPlugins(config, (Collection<SyncPlugin>) (Collection<?>) inputPlugins.values(), args)
                && PluginLoader.initPlugins(config, (Collection<SyncPlugin>) (Collection<?>) outputPlugins.values(), args);
    }

    public static String getHelpMessages(StringBuilder helpMessages) {
        helpMessages.append("\nInput Plugins:\n");
        for (SyncInputPlugin inputPlugin : inputPlugins.values()) {
            helpMessages.append(inputPlugin.helpScreen());
        }
        helpMessages.append("Output Plugins:\n");
        for (SyncOutputPlugin outputPlugin : outputPlugins.values()) {
            helpMessages.append(outputPlugin.helpScreen());
        }
        return helpMessages.toString();
    }

    private static boolean initPlugins(Configuration config, Collection<SyncPlugin> plugins, String[] args) throws Exception {
        for (SyncPlugin plugin : plugins) {

            for (PluginConfiguration pluginConfig : config.getPlugins()) {
                if (StringUtils.equals(pluginConfig.getPlugin(), plugin.getSchema())) {
                    plugin.init(pluginConfig.getConfig());
                    break;
                }
            }

            if (!plugin.parseArguments(args)) {
                return false;
            }
        }
        return true;
    }

    private static void loadInputPlugins() throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        loadPlugins(inputPlugins, SyncInputPlugin.class);
    }

    private static void loadOutputPlugins() throws IllegalAccessException, InstantiationException, InvocationTargetException, NoSuchMethodException {
        loadPlugins(outputPlugins, SyncOutputPlugin.class);
    }

    private static <T> void loadPlugins(Map<String, T> map, Class<T> clazz) throws IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        Reflections reflections = new Reflections("rocks.voss.musicsync.plugins");
        Set<Class<? extends T>> subTypes = reflections.getSubTypesOf(clazz);
        Iterator<Class<? extends T>> iterator = subTypes.iterator();
        while (iterator.hasNext()) {
            T instance = iterator.next().getDeclaredConstructor().newInstance(new Object[]{});
            map.put(((SyncPlugin) instance).getSchema(), instance);
        }
    }
}

