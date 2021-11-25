package rocks.voss.musicsync.application.config;

import lombok.Data;

@Data
public class ConnectionEndpoint {
    private String plugin;
    private Object config;
}
