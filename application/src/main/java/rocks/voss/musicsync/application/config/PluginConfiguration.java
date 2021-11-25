package rocks.voss.musicsync.application.config;

import lombok.Data;

@Data
public class PluginConfiguration {
    private String plugin;
    private Object config;
}
