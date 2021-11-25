package rocks.voss.musicsync.api;

public interface SyncPlugin {
    void establishConnection();

    void init(Object configuration) throws Exception;

    boolean parseArguments(String[] args) throws Exception;

    String getSchema();

    void closeConnection();
}
