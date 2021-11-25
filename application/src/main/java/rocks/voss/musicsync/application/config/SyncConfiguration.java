package rocks.voss.musicsync.application.config;

import lombok.Data;

@Data
public class SyncConfiguration {
    private ConnectionEndpoint in;
    private ConnectionEndpoint out;
}
