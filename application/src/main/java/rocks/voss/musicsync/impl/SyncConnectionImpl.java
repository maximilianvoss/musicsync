package rocks.voss.musicsync.impl;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import rocks.voss.musicsync.PluginLoader;
import rocks.voss.musicsync.api.SyncConnection;
import rocks.voss.musicsync.api.SyncInputPlugin;
import rocks.voss.musicsync.api.SyncOutputPlugin;

@Data
public class SyncConnectionImpl implements SyncConnection {
    private String inputUri;
    private String outputUri;
    private SyncInputPlugin syncInputPlugin = null;
    private SyncOutputPlugin syncOutputPlugin = null;

    public static SyncConnectionImpl createByUris(String inputUri, String outputUri) {
        SyncConnectionImpl syncConnectionImpl = new SyncConnectionImpl();
        syncConnectionImpl.setInputUri(inputUri);
        syncConnectionImpl.setOutputUri(outputUri);
        return syncConnectionImpl;
    }

    @Override
    public String getInputSchema() {
        return StringUtils.substring(inputUri, 0,StringUtils.indexOf(inputUri, ":"));
    }

    @Override
    public String getOutputSchema() {
        return StringUtils.substring(outputUri, 0,StringUtils.indexOf(outputUri, ":"));
    }

    public SyncInputPlugin getSyncInputPlugin() {
        if (this.syncInputPlugin == null) {
            this.syncInputPlugin = PluginLoader.getInputPlugins().get(getInputSchema());
        }
        return this.syncInputPlugin;
    }

    public SyncOutputPlugin getSyncOutputPlugin() {
        if (this.syncOutputPlugin == null) {
            this.syncOutputPlugin = PluginLoader.getOutputPlugins().get(getOutputSchema());
        }
        return this.syncOutputPlugin;
    }
}
