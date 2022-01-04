package rocks.voss.musicsync.plugins.spotify;

import lombok.Data;
import org.apache.hc.core5.http.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rocks.voss.musicsync.api.SyncPlugin;
import rocks.voss.musicsync.api.SyncTrack;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.ArtistSimplified;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.model_objects.specification.Track;
import se.michaelthelin.spotify.requests.data.tracks.GetTrackRequest;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
public class SpotifySyncTrackImpl implements SyncTrack {
    final private static Logger log = LogManager.getLogger(SpotifySyncTrackImpl.class);

    private SyncPlugin plugin;
    private String id;
    private String uri;
    private String[] artists;
    private String name;
    private int trackNumber;
    private int trackDuration;
    private String album;
    private boolean fresh = false;

    private PlaylistTrack spotifyTrack;

    public static SyncTrack createBy(SpotifyInputPlugin spotifyInputPlugin, PlaylistTrack playlistTrack, int order) throws IOException, ParseException, SpotifyWebApiException {
        SpotifySyncTrackImpl syncTrack = new SpotifySyncTrackImpl();

        GetTrackRequest getTrackRequest = spotifyInputPlugin.getPluginConfiguration().getSpotifyApi().getTrack(playlistTrack.getTrack().getId()).build();
        final Track track = getTrackRequest.execute();

        List<String> artists = new ArrayList<>();
        for (ArtistSimplified artist : track.getArtists()) {
            artists.add(artist.getName());
        }

        syncTrack.setPlugin(spotifyInputPlugin);
        syncTrack.setId(playlistTrack.getTrack().getId());
        syncTrack.setUri(playlistTrack.getTrack().getUri());
        syncTrack.setArtists(artists.toArray(new String[]{}));
        syncTrack.setName(playlistTrack.getTrack().getName());
        syncTrack.setTrackNumber(order);
        syncTrack.setTrackDuration(track.getDurationMs());
        syncTrack.setAlbum(track.getAlbum().getName());

        syncTrack.setSpotifyTrack(playlistTrack);

        return syncTrack;
    }

    public boolean isFresh() {
        SpotifyInputPlugin spotifyInputPlugin = (SpotifyInputPlugin) getPlugin();
        File fileDestination = getFileDestination(spotifyInputPlugin);

        SpotifyRecordingHandler.isFileValid(spotifyInputPlugin.getPluginConfiguration(), spotifyTrack, fileDestination);
        return false;
    }

    public String getPath() {
        SpotifyInputPlugin spotifyInputPlugin = (SpotifyInputPlugin) getPlugin();
        File fileDestination = getFileDestination(spotifyInputPlugin);
        try {
            fresh = SpotifyRecordingHandler.recordTrack(spotifyInputPlugin.getPluginConfiguration(), spotifyTrack, fileDestination);
        } catch (IOException | InterruptedException e) {
            log.error(e);
        }
        return fileDestination.getAbsolutePath();
    }

    private File getFileDestination(SpotifyInputPlugin spotifyInputPlugin) {
        String cachePath = spotifyInputPlugin.getPluginConfiguration().getCachePath() + "/" + getId() + ".mp3";
        return new File(cachePath);
    }
}
