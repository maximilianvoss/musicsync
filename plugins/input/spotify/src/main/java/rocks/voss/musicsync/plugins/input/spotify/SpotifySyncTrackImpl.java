package rocks.voss.musicsync.plugins.input.spotify;

import lombok.Data;
import org.apache.hc.core5.http.ParseException;
import rocks.voss.musicsync.api.SyncTrack;
import rocks.voss.musicsync.plugins.input.spotify.config.PluginConfiguration;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.data.tracks.GetTrackRequest;

import java.io.IOException;
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

    public static SyncTrack createBy(PluginConfiguration spotifyHandler, PlaylistTrack playlistTrack, int order) throws IOException, ParseException, SpotifyWebApiException {
        SpotifySyncTrackImpl syncTrack = new SpotifySyncTrackImpl();

        GetTrackRequest getTrackRequest = spotifyHandler.getSpotifyApi().getTrack(playlistTrack.getTrack().getId()).build();
        final Track track = getTrackRequest.execute();

        List<String> artists = new ArrayList<>();
        for (ArtistSimplified artist : track.getArtists()) {
            artists.add(artist.getName());
        }

        syncTrack.setId(playlistTrack.getTrack().getId());
        syncTrack.setUri(playlistTrack.getTrack().getUri());
        syncTrack.setArtists(artists.toArray(new String[]{}));
        syncTrack.setName(playlistTrack.getTrack().getName());
        syncTrack.setDiscNumber(track.getDiscNumber());
        syncTrack.setTrackNumber(order);
        syncTrack.setAlbumName(track.getAlbum().getName());
        syncTrack.setOriginalTrack(playlistTrack);
        syncTrack.setCacheLocation(spotifyHandler.getCachePath() + "/" + playlistTrack.getTrack().getId() + ".mp3");

        return syncTrack;
    }
}
