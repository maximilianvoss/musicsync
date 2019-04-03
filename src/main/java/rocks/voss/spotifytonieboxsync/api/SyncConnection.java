package rocks.voss.spotifytonieboxsync.api;

public interface SyncConnection {
    String getInputUri();
    String getInputSchema();
    SyncInputPlugin getSyncInputPlugin();
    void setSyncInputPlugin(SyncInputPlugin syncInputPlugin);

    String getOutputUri();
    String getOutputSchema();
    SyncOutputPlugin getSyncOutputPlugin();
    void setSyncOutputPlugin(SyncOutputPlugin syncOutputPlugin);
}
