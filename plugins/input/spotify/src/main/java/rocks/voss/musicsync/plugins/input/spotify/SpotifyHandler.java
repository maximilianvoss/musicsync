package rocks.voss.musicsync.plugins.input.spotify;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.SpotifyHttpManager;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;

import java.net.URI;
import java.util.Properties;

@Data
public class SpotifyHandler {
    private String clientId;
    private String clientSecret;
    private URI redirectUri;
    private SpotifyApi spotifyApi = null;
    private AuthorizationCodeRequest authorizationCodeRequest;
    private String accessToken;
    private String refreshToken;
    private String cachePath;
    private int trackThreshold = 1000;

    public static SpotifyHandler createHandlerByProperties(Properties properties) {
        SpotifyHandler spotifyHandler = new SpotifyHandler();
        spotifyHandler.setClientId(properties.getProperty("spotify.clientId"));
        spotifyHandler.setClientSecret(properties.getProperty("spotify.clientSecret"));
        spotifyHandler.setRedirectUri(SpotifyHttpManager.makeUri(properties.getProperty("spotify.redirectUri")));
        spotifyHandler.setRefreshToken(properties.getProperty("spotify.refreshToken"));
        spotifyHandler.setCachePath(properties.getProperty("spotify.cachePath"));
        String trackThreshold = properties.getProperty("spotify.trackThreshold");
        if (trackThreshold != null) {
            spotifyHandler.setTrackThreshold(Integer.valueOf(trackThreshold));
        }
        return spotifyHandler;
    }

    public SpotifyApi getSpotifyApi() {
        if (this.spotifyApi == null) {
            SpotifyApi.Builder builder = new SpotifyApi.Builder()
                    .setClientId(clientId)
                    .setClientSecret(clientSecret)
                    .setRedirectUri(redirectUri);
            if (StringUtils.isNotBlank(accessToken)) {
                builder.setAccessToken(accessToken);
            }
            if (StringUtils.isNotBlank(refreshToken)) {
                builder.setRefreshToken(refreshToken);
            }
            this.spotifyApi = builder.build();
        }
        return spotifyApi;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        this.spotifyApi = null;
    }
}
