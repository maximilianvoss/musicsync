package rocks.voss.musicsync.plugins.input.spotify;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.core5.http.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rocks.voss.jsonhelper.JSONHelper;
import rocks.voss.musicsync.api.SyncConnection;
import rocks.voss.musicsync.api.SyncInputPlugin;
import rocks.voss.musicsync.api.SyncTrack;
import rocks.voss.musicsync.plugins.input.spotify.config.PluginConfiguration;
import rocks.voss.musicsync.plugins.input.spotify.config.SyncConfiguration;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.lang.System.exit;

public class SpotifyInputPlugin implements SyncInputPlugin {
    final private static Logger log = LogManager.getLogger(SpotifyInputPlugin.class);

    @Getter
    private PluginConfiguration pluginConfiguration;

    @Override
    public void establishConnection() {
        try {
            SpotifyAuthenticationSetup.refreshToken(pluginConfiguration);
        } catch (IOException e) {
            log.error("IOException", e);
        } catch (SpotifyWebApiException e) {
            log.error("SpotifyWebApiException", e);
        } catch (ParseException e) {
            log.error("ParseException", e);
        }
    }

    @Override
    public void init(Object configuration) {
        try {
            pluginConfiguration = JSONHelper.createBean(PluginConfiguration.class, configuration);
        } catch (IOException e) {
            log.error(e);
        }
    }

    @Override
    public boolean parseArguments(String[] args) {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (StringUtils.equals(arg, "--spotify-apicode") && args.length == 1) {
                log.debug("--spotify-apicode");
                SpotifyAuthenticationSetup.getSignInUrl(pluginConfiguration);
                exit(0);
                return true;
            } else if (StringUtils.equals(arg, "--spotify-code") && args.length == 2) {
                String code = args[++i];
                log.debug("--spotify-code: " + code);
                try {
                    SpotifyAuthenticationSetup.getAccessToken(pluginConfiguration, code);
                } catch (IOException | SpotifyWebApiException | ParseException e) {
                    log.error(e);
                }
                exit(0);
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
                "--spotify-code CODE\n\t\tGenerate a Refresh token out of the Spotify Code\n\n";
    }

    @Override
    public List<SyncTrack> getTracklist(SyncConnection connection) {
        List<SyncTrack> syncTracks = new ArrayList<>();

        try {
            SyncConfiguration syncConfig = JSONHelper.createBean(SyncConfiguration.class, connection.getInputConfig());

            List<PlaylistSimplified> playlists = PlaylistHandler.getPlaylists(pluginConfiguration);
            for (PlaylistSimplified playlist : playlists) {
                log.trace("Playlist id: " + playlist.getUri());
                if (StringUtils.equals(playlist.getUri(), syncConfig.getUri())) {
                    log.trace("Playlist match: " + syncConfig.getUri());
                    List<PlaylistTrack> tracks = PlaylistHandler.getTracks(pluginConfiguration, playlist);
                    for (int i = 0; i < tracks.size(); i++) {
                        PlaylistTrack track = tracks.get(i);
                        syncTracks.add(SpotifySyncTrackImpl.createBy(this, track, i));
                    }
                    break;
                }
            }
        } catch (Exception e) {
            log.error("Exception", e);
        }
        return syncTracks;
    }

    @Override
    public String getSchema() {
        return "spotify";
    }

    @Override
    public void closeConnection() {
        return;
    }
}
