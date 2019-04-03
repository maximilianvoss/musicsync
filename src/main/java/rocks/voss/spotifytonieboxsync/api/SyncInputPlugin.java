package rocks.voss.spotifytonieboxsync.api;

import java.util.List;

public interface SyncInputPlugin extends SyncPlugin{
    String helpScreen();
    List<SyncTrack> getTracklist(SyncConnection connection);
    void downloadTracks(SyncConnection connection, List<SyncTrack> tracks);
}
