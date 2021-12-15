package rocks.voss.musicsync.api;

import java.util.List;

public interface SyncOutputPlugin extends SyncPlugin {
    /**
     * Upload a full list of tracks
     *
     * @param connection to target destination
     * @param syncTracks is a list of tracks
     */
    void uploadTracks(SyncConnection connection, List<SyncTrack> syncTracks);

    /**
     * Upload a single track to outbound plugin
     *
     * @param connection to target destination
     * @param syncTrack  is a track to be uploaded
     */
    void uploadTrack(SyncConnection connection, SyncTrack syncTrack);

    /**
     * Check if a track is already uploaded to target destination, can also validate if a track needs re-uploading
     *
     * @param connection to target destination
     * @param syncTrack  track to be checked for upload
     * @return
     */
    boolean isTrackUploaded(SyncConnection connection, SyncTrack syncTrack);

    /**
     * Order tracks on target destination
     *
     * @param connection to target destination
     * @param syncTracks is a full list of all tracks to make the sorting
     */
    void orderTracks(SyncConnection connection, List<SyncTrack> syncTracks);

    /**
     * to target destination
     *
     * @param connection to target destination
     * @param syncTracks list of all Tracks, track which are not in the list will be removed
     */
    void cleanUpTracks(SyncConnection connection, List<SyncTrack> syncTracks);
}
