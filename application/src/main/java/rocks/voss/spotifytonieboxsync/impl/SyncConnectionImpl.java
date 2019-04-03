package rocks.voss.spotifytonieboxsync.impl;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import rocks.voss.spotifytonieboxsync.api.SyncConnection;
import rocks.voss.spotifytonieboxsync.api.SyncInputPlugin;
import rocks.voss.spotifytonieboxsync.api.SyncOutputPlugin;

@Data
public class SyncConnectionImpl implements SyncConnection {
    private String inputUri;
    private String outputUri;
    private SyncInputPlugin syncInputPlugin;
    private SyncOutputPlugin syncOutputPlugin;

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

}
