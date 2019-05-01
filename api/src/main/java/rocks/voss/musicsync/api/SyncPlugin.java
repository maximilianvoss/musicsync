package rocks.voss.musicsync.api;

import java.util.Properties;

public interface SyncPlugin {
    void establishConnection();

    void init(Properties properties) throws Exception;

    boolean parseArguments(String[] args) throws Exception;

    String getSchema();
}
