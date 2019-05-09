package rocks.voss.musicsync.plugins.input.spotify;

import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.PlaylistSimplified;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import rocks.voss.musicsync.api.SyncConnection;
import rocks.voss.musicsync.api.SyncInputPlugin;
import rocks.voss.musicsync.api.SyncTrack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class SpotifyInputPlugin implements SyncInputPlugin {
    final private Logger log = Logger.getLogger(this.getClass().getName());
    private SpotifyHandler spotifyHandler;

    @Override
    public void establishConnection() {
        try {
            SpotifyAuthenticationSetup.refreshToken(spotifyHandler);
        } catch (IOException e) {
            log.error("IOException", e);
        } catch (SpotifyWebApiException e) {
            log.error("SpotifyWebApiException", e);
        }
    }

    @Override
    public void init(Properties properties) {
        spotifyHandler = SpotifyHandler.createHandlerByProperties(properties);
    }

    @Override
    public boolean parseArguments(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (StringUtils.equals(arg, "--spotify-apicode") && args.length == 1) {
                log.debug("--spotify-apicode");
                SpotifyAuthenticationSetup.getSignInUrl(spotifyHandler);
                return true;
            } else if (StringUtils.equals(arg, "--spotify-code") && args.length == 2) {
                String code = args[++i];
                log.debug("--spotify-code: " + code);
                SpotifyAuthenticationSetup.getAccessToken(spotifyHandler, code);
                return true;
            } else if (StringUtils.equals(arg, "--spotify-apicode") && args.length != 1) {
                log.debug("--spotify-apicode has wrong argument count");
                return false;
            } else if (StringUtils.equals(arg, "--spotify-code") && args.length != 2) {
                log.debug("--spotify-code has wrong argument count");
                return false;
            }
        }
        log.debug("Parsing arguments is okay");
        return true;
    }

    @Override
    public String helpScreen() {
        return "--spotify-apicode\n\t\tGenerate an API Code URL to get attach Application to Spotify Account\n" +
                "--spotify-code CODE\n\t\tGenerate a Refresh token out of the Spotify Code\n";
    }

    @Override
    public List<SyncTrack> getTracklist(SyncConnection connection) {
        List<SyncTrack> syncTracks = new ArrayList<>();
        try {
            List<PlaylistSimplified> playlists = PlaylistHandler.getPlaylists(spotifyHandler);
            for (PlaylistSimplified playlist : playlists) {
                log.trace("Playlist id: " + playlist.getUri());
                if (StringUtils.equals(playlist.getUri(), connection.getInputUri())) {
                    log.trace("Playlist match: " + connection.getInputUri());
                    List<PlaylistTrack> tracks = PlaylistHandler.getTracks(spotifyHandler, playlist);
                    for (int i = 0; i < tracks.size(); i++) {
                        PlaylistTrack track = tracks.get(i);
                        syncTracks.add(SpotifySyncTrackImpl.createBy(spotifyHandler, track, i));
                    }
                    return syncTracks;
                }
            }
        } catch (IOException e) {
            log.error("IOException", e);
        } catch (SpotifyWebApiException e) {
            log.error("SpotifyWebApiException", e);
        }
        return syncTracks;
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
