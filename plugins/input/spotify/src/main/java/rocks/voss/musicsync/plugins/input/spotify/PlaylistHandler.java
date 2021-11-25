package rocks.voss.musicsync.plugins.input.spotify;

import org.apache.hc.core5.http.ParseException;
import rocks.voss.musicsync.plugins.input.spotify.config.PluginConfiguration;
import se.michaelthelin.spotify.exceptions.SpotifyWebApiException;
import se.michaelthelin.spotify.model_objects.specification.Paging;
import se.michaelthelin.spotify.model_objects.specification.PlaylistSimplified;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;
import se.michaelthelin.spotify.requests.data.playlists.GetListOfCurrentUsersPlaylistsRequest;
import se.michaelthelin.spotify.requests.data.playlists.GetPlaylistsItemsRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class PlaylistHandler {
    public static List<PlaylistSimplified> getPlaylists(PluginConfiguration spotifyHandler)
            throws IOException, SpotifyWebApiException, ParseException {
        int offset = 0;
        int limit = 10;
        List<PlaylistSimplified> playlists = new ArrayList<>();
        Paging<PlaylistSimplified> playlistSimplifiedPaging;
        do {
            GetListOfCurrentUsersPlaylistsRequest getListOfCurrentUsersPlaylistsRequest = spotifyHandler.getSpotifyApi()
                    .getListOfCurrentUsersPlaylists()
                    .limit(limit)
                    .offset(offset)
                    .build();

            playlistSimplifiedPaging = getListOfCurrentUsersPlaylistsRequest.execute();
            playlists.addAll(Arrays.asList(playlistSimplifiedPaging.getItems()));
            offset += limit;
        } while (offset < playlistSimplifiedPaging.getTotal());
        return playlists;
    }

    public static List<PlaylistTrack> getTracks(PluginConfiguration spotifyHandler, PlaylistSimplified playlist)
            throws IOException, SpotifyWebApiException, ParseException {
        int offset = 0;
        int limit = 10;
        List<PlaylistTrack> tracks = new ArrayList<>();
        Paging<PlaylistTrack> playlistTrackPaging;
        do {
            GetPlaylistsItemsRequest getPlaylistsItemsRequest = spotifyHandler.getSpotifyApi().getPlaylistsItems(playlist.getId()).build();
            playlistTrackPaging = getPlaylistsItemsRequest.execute();
            tracks.addAll(Arrays.asList(playlistTrackPaging.getItems()));
            offset += limit;
        } while (offset < playlistTrackPaging.getTotal());
        return tracks;
    }
}
