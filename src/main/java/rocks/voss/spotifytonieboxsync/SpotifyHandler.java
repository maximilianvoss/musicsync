package rocks.voss.spotifytonieboxsync;

import com.wrapper.spotify.SpotifyApi;
import com.wrapper.spotify.SpotifyHttpManager;
import com.wrapper.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

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

    public static SpotifyHandler createHandlerByProperties(Properties properties) {
        SpotifyHandler spotifyHandler = new SpotifyHandler();
        spotifyHandler.setClientId(properties.getProperty("clientId"));
        spotifyHandler.setClientSecret(properties.getProperty("clientSecret"));
        spotifyHandler.setRedirectUri(SpotifyHttpManager.makeUri(properties.getProperty("redirectUri")));
        spotifyHandler.setAccessToken(properties.getProperty("accessToken"));
        spotifyHandler.setRefreshToken(properties.getProperty("refreshToken"));
        spotifyHandler.setCachePath(properties.getProperty("cachePath"));
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
