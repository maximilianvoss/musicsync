package rocks.voss.spotifytonieboxsync;

import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.Paging;
import com.wrapper.spotify.model_objects.specification.PlaylistSimplified;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import com.wrapper.spotify.requests.data.playlists.GetListOfCurrentUsersPlaylistsRequest;
import com.wrapper.spotify.requests.data.playlists.GetPlaylistsTracksRequest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

class PlaylistHandler {
    public static List<PlaylistSimplified> getPlaylists(SpotifyHandler spotifyHandler)
            throws IOException, SpotifyWebApiException {
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

    public static List<PlaylistTrack> getTracks(SpotifyHandler spotifyHandler, PlaylistSimplified playlist)
            throws IOException, SpotifyWebApiException {
        int offset = 0;
        int limit = 10;
        List<PlaylistTrack> tracks = new ArrayList<>();
        Paging<PlaylistTrack> playlistTrackPaging;
        do {
            GetPlaylistsTracksRequest getPlaylistsTracksRequest = spotifyHandler.getSpotifyApi()
                    .getPlaylistsTracks(playlist.getId())
                    .limit(limit)
                    .offset(offset)
                    .build();

            playlistTrackPaging = getPlaylistsTracksRequest.execute();
            tracks.addAll(Arrays.asList(playlistTrackPaging.getItems()));
            offset += limit;
        } while (offset < playlistTrackPaging.getTotal());
        return tracks;
    }
}
