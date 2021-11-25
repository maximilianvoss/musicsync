package rocks.voss.musicsync.application.config;

import lombok.Data;

import java.util.List;

@Data
public class Configuration {
    private GeneralConfiguration general;
    private List<PluginConfiguration> plugins;
    private List<SyncConfiguration> connections;
}
