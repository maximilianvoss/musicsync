package rocks.voss.musicsync.api;

public interface SyncConnection {
    String getInputUri();
    String getInputSchema();
    SyncInputPlugin getSyncInputPlugin();
    String getOutputUri();
    String getOutputSchema();
    SyncOutputPlugin getSyncOutputPlugin();
}
