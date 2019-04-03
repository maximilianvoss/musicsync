package rocks.voss.musicsync.api;

import java.util.Properties;

public interface SyncPlugin {
    void init(Properties properties) throws Exception;
    boolean parseArguments(String[] args) throws Exception;
    String getSchema();
}
