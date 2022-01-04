package rocks.voss.musicsync.plugins.spotify;

import org.apache.hc.core5.http.ParseException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rocks.voss.musicsync.plugins.spotify.config.PluginConfiguration;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.credentials.AuthorizationCodeCredentials;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRefreshRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeUriRequest;

import java.io.IOException;
import java.net.URI;

public class SpotifyAuthenticationSetup {
    final private static Logger log = LogManager.getLogger(SpotifyAuthenticationSetup.class);

    public static void getAccessToken(PluginConfiguration spotifyHandler, String code)
            throws IOException, SpotifyWebApiException, ParseException {
        AuthorizationCodeRequest authorizationCodeRequest = spotifyHandler.getSpotifyApi().authorizationCode(code)
                .build();
        final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRequest.execute();
        spotifyHandler.getSpotifyApi().setAccessToken(authorizationCodeCredentials.getAccessToken());
        spotifyHandler.getSpotifyApi().setRefreshToken(authorizationCodeCredentials.getRefreshToken());

        log.info("Refresh Token: " + authorizationCodeCredentials.getRefreshToken());
        System.out.println("Refresh Token: " + authorizationCodeCredentials.getRefreshToken());
    }

    public static void getSignInUrl(PluginConfiguration spotifyHandler) {
        AuthorizationCodeUriRequest authorizationCodeUriRequest = spotifyHandler.getSpotifyApi().authorizationCodeUri()
                .state("x4xkmn9pu3j6ukrs8n")
                .scope("playlist-read-private,playlist-read-collaborative,playlist-modify-private,playlist-modify-public")
                .show_dialog(true)
                .build();
        final URI uri = authorizationCodeUriRequest.execute();
        log.info("URI: " + uri.toString());
        System.out.println("URI: " + uri.toString());
    }


    public static void refreshToken(PluginConfiguration spotifyHandler)
            throws IOException, SpotifyWebApiException, ParseException {
        AuthorizationCodeRefreshRequest authorizationCodeRefreshRequest = spotifyHandler.getSpotifyApi().authorizationCodeRefresh().build();
        final AuthorizationCodeCredentials authorizationCodeCredentials = authorizationCodeRefreshRequest.execute();

        spotifyHandler.getSpotifyApi().setAccessToken(authorizationCodeCredentials.getAccessToken());
        spotifyHandler.getSpotifyApi().setRefreshToken(authorizationCodeCredentials.getRefreshToken());
        spotifyHandler.setAccessToken(authorizationCodeCredentials.getAccessToken());
    }
}
