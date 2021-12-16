package rocks.voss.musicsync.api;

public interface SyncTrack {
    /**
     * @return plugin which is used to retrieve this track
     */
    SyncPlugin getPlugin();

    /**
     * @return track identifiere
     */
    String getId();

    /**
     * @return URI to track
     */
    String getUri();

    /**
     * @return list of artists
     */
    String[] getArtists();

    /**
     * @return track name
     */
    String getName();

    /**
     * @return track number on album
     */
    int getTrackNumber();

    /**
     * @return length of track
     */
    int getTrackDuration();

    /**
     * @return album name
     */
    String getAlbum();

    /**
     * @return path to file in filesystem where track is stored
     */
    String getPath();

    /**
     * @return in indicator if the file is freshly downloaded or if cache is updated
     */
    boolean isFresh();
}
