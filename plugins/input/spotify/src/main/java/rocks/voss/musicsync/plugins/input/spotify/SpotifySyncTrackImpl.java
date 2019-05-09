package rocks.voss.musicsync.plugins.input.spotify;

import com.wrapper.spotify.model_objects.specification.ArtistSimplified;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import lombok.Data;
import rocks.voss.musicsync.api.SyncTrack;

import java.util.ArrayList;
import java.util.List;

@Data
public class SpotifySyncTrackImpl implements SyncTrack {
    private String id;
    private String uri;
    private String[] artists;
    private String name;
    private int discNumber;
    private int trackNumber;
    private String albumName;
    private Object originalTrack;
    private String cacheLocation;

    public static SyncTrack createBy(SpotifyHandler spotifyHandler, PlaylistTrack track, int order) {
        SpotifySyncTrackImpl syncTrack = new SpotifySyncTrackImpl();

        List<String> artists = new ArrayList<>();
        for (ArtistSimplified artist : track.getTrack().getArtists()) {
            artists.add(artist.getName());
        }

        syncTrack.setId(track.getTrack().getId());
        syncTrack.setUri(track.getTrack().getUri());
        syncTrack.setArtists(artists.toArray(new String[]{}));
        syncTrack.setName(track.getTrack().getName());
        syncTrack.setDiscNumber(track.getTrack().getDiscNumber());
        syncTrack.setTrackNumber(order);
        syncTrack.setAlbumName(track.getTrack().getAlbum().getName());
        syncTrack.setOriginalTrack(track);
        syncTrack.setCacheLocation(spotifyHandler.getCachePath() + "/" + track.getTrack().getId() + ".mp3");

        return syncTrack;

    }
}
