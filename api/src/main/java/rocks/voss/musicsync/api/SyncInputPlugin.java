package rocks.voss.musicsync.api;

import java.util.List;

public interface SyncInputPlugin extends SyncPlugin {
    /**
     * Get the track list for the connection
     *
     * @param connection for which to get the tracklist from
     * @return list of tracks
     */
    List<SyncTrack> getTracklist(SyncConnection connection);
}
