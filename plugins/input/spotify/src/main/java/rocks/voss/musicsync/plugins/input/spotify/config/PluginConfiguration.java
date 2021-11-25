package rocks.voss.musicsync.plugins.input.spotify.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import se.michaelthelin.spotify.SpotifyApi;
import se.michaelthelin.spotify.requests.authorization.authorization_code.AuthorizationCodeRequest;

import java.net.URI;

@Data
public class PluginConfiguration {
    private String clientId;
    private String clientSecret;
    private URI redirectUri;
    private String accessToken;
    private String refreshToken;
    private String cachePath;
    private int trackThreshold = 1000;

    @JsonIgnore
    private SpotifyApi spotifyApi = null;

    @JsonIgnore
    private AuthorizationCodeRequest authorizationCodeRequest;

    @JsonIgnore
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

    @JsonIgnore
    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
        this.spotifyApi = null;
    }
}
