package rocks.voss.spotifytonieboxsync.plugins.input.spotify;

import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.PlaylistSimplified;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import rocks.voss.spotifytonieboxsync.api.SyncConnection;
import rocks.voss.spotifytonieboxsync.api.SyncInputPlugin;
import rocks.voss.spotifytonieboxsync.api.SyncTrack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SpotifyInputPlugin implements SyncInputPlugin {
    final private Logger log = Logger.getLogger(this.getClass().getName());
    SpotifyHandler spotifyHandler;

    @Override
    public void init(Properties properties) {
        spotifyHandler = SpotifyHandler.createHandlerByProperties(properties);
    }

    @Override
    public boolean parseArguments(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (StringUtils.equals(arg, "--apicode") && args.length == 1) {
                log.trace("--apicode");
                SpotifyAuthenticationSetup.getSignInUrl(spotifyHandler);
                return true;
            } else if (StringUtils.equals(arg, "--code") && args.length == 2) {
                String code = args[++i];
                log.trace("--code: " + code);
                SpotifyAuthenticationSetup.getAccessToken(spotifyHandler, code);
                return true;
            } else if (StringUtils.equals(arg, "--apicode") && args.length != 1) {
                return false;
            } else if (StringUtils.equals(arg, "--code") && args.length != 2) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String helpScreen() {
        return "--apicode\n\t\tGenerate an API Code URL to get attach Application to Spotify Account\n" +
                "--code CODE\n\t\tGenerate a Refresh token out of the Spotify Code\n";
    }

    @Override
    public List<SyncTrack> getTracklist(SyncConnection connection) {
        List<SyncTrack> tracks = new ArrayList<>();
        try {
            SpotifyAuthenticationSetup.refreshToken(spotifyHandler);
            List<PlaylistSimplified> playlists = PlaylistHandler.getPlaylists(spotifyHandler);
            for (PlaylistSimplified playlist : playlists) {
                log.trace("Playlist id: " + playlist.getUri());
                if (StringUtils.equals(playlist.getUri(), connection.getInputUri())) {
                    log.trace("Playlist match: " + connection.getInputUri());
                    List<PlaylistTrack> trackList = PlaylistHandler.getTracks(spotifyHandler, playlist);
                    for ( PlaylistTrack track : trackList ){
                        tracks.add(SpotifySyncTrackImpl.createBy(spotifyHandler, track));
                    }
                    return tracks;
                }
            }
        } catch (IOException e) {
            log.error("IOException", e);
        } catch (SpotifyWebApiException e) {
            log.error("SpotifyWebApiException", e);
        }
        return tracks;
    }

    @Override
    public void downloadTracks(SyncConnection connection, List<SyncTrack> syncTracks) {
        for ( SyncTrack track : syncTracks) {
            try {
                SpotifyRecordingHandler.recordTrack(spotifyHandler, (PlaylistTrack) track.getOriginalTrack(), track.getCacheLocation());
            } catch (IOException e) {
                log.error("IOException", e);
            } catch (InterruptedException e) {
                log.error("InterruptedException", e);
            }
        }
    }

    @Override
    public String getSchema() {
        return "spotify";
    }
}
