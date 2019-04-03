package rocks.voss.spotifytonieboxsync.api;

import java.util.List;

public interface SyncOutputPlugin extends SyncPlugin{
    String helpScreen();
    void uploadTracks(SyncConnection connection, List<SyncTrack> syncTracks);
    boolean isTrackUploaded(SyncConnection connection, SyncTrack syncTrack);
    void orderTracks(SyncConnection connection, List<SyncTrack> syncTracks);
    void cleanUpTracks(SyncConnection connection, List<SyncTrack> syncTracks);
}
