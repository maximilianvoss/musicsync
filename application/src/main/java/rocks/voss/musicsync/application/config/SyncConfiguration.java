package rocks.voss.musicsync.application.config;

import lombok.Data;

@Data
public class SyncConfiguration {
    private String name;
    private ConnectionEndpoint in;
    private ConnectionEndpoint out;
}
