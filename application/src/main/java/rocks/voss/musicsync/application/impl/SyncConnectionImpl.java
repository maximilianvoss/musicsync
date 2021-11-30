package rocks.voss.musicsync.application.impl;

import lombok.Getter;
import lombok.Setter;
import rocks.voss.musicsync.api.SyncConnection;
import rocks.voss.musicsync.api.SyncInputPlugin;
import rocks.voss.musicsync.api.SyncOutputPlugin;
import rocks.voss.musicsync.application.PluginLoader;
import rocks.voss.musicsync.application.config.ConnectionEndpoint;

@Getter
public class SyncConnectionImpl implements SyncConnection {
    private SyncInputPlugin syncInputPlugin = null;
    private SyncOutputPlugin syncOutputPlugin = null;

    @Setter
    private ConnectionEndpoint in;
    @Setter
    private ConnectionEndpoint out;
    @Setter
    private String name;

    public static SyncConnection createBy(String name, ConnectionEndpoint in, ConnectionEndpoint out) {
        SyncConnectionImpl connection = new SyncConnectionImpl();
        connection.setIn(in);
        connection.setOut(out);
        connection.setName(name);

        return connection;
    }

    @Override
    public String getInputSchema() {
        return in.getPlugin();
    }

    @Override
    public String getOutputSchema() {
        return out.getPlugin();
    }

    public SyncInputPlugin getSyncInputPlugin() {
        if (this.syncInputPlugin == null) {
            this.syncInputPlugin = PluginLoader.getInputPlugins().get(getInputSchema());
        }
        return this.syncInputPlugin;
    }

    @Override
    public Object getInputConfig() {
        return in.getConfig();
    }

    public SyncOutputPlugin getSyncOutputPlugin() {
        if (this.syncOutputPlugin == null) {
            this.syncOutputPlugin = PluginLoader.getOutputPlugins().get(getOutputSchema());
        }
        return this.syncOutputPlugin;
    }

    @Override
    public Object getOutputConfig() {
        return out.getConfig();
    }

    @Override
    public String getName() {
        return null;
    }
}
