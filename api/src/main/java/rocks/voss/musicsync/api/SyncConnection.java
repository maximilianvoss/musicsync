package rocks.voss.musicsync.api;

public interface SyncConnection {
    String getInputSchema();
    SyncInputPlugin getSyncInputPlugin();

    Object getInputConfig();

    String getOutputSchema();

    SyncOutputPlugin getSyncOutputPlugin();

    Object getOutputConfig();
}
