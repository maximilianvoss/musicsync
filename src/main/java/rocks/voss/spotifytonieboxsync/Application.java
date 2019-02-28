package rocks.voss.spotifytonieboxsync;

import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.PlaylistSimplified;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import rocks.voss.toniebox.TonieHandler;
import rocks.voss.toniebox.beans.Tonie;
import rocks.voss.toniebox.beans.toniebox.TonieChapterBean;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Application {
    private static Logger log = Logger.getLogger(Application.class.getName());

    public static void main(String[] args) {
        try {
            Properties spotifyProperties = getProperties("spotify.properties");
            Properties tonieboxProperties = getProperties("toniebox.properties");
            Properties spotifytonieboxsyncProperties = getProperties("spotifytonieboxsync.properties");
            SpotifyHandler spotifyHandler = SpotifyHandler.createHandlerByProperties(spotifyProperties);

            String playlistName = null;
            String tonieName = null;
            boolean isDaemon = false;
            for (int i = 0; i < args.length; i++) {
                String arg = args[i];
                if (StringUtils.equals(arg, "--apicode") && args.length == 1) {
                    log.trace("--apicode");
                    SpotifyAuthenticationSetup.getSignInUrl(spotifyHandler);
                    return;
                } else if (StringUtils.equals(arg, "--code") && args.length == 2) {
                    String code = args[++i];
                    log.trace("--code: " + code);
                    SpotifyAuthenticationSetup.getAccessToken(spotifyHandler, code);
                    return;
                } else if (StringUtils.equals(arg, "--playlist") && args.length == 4) {
                    playlistName = args[++i];
                    log.trace("--playlist: " + playlistName);
                } else if (StringUtils.equals(arg, "--tonie") && args.length == 4) {
                    tonieName = args[++i];
                    log.trace("--tonie: " + tonieName);
                } else if (StringUtils.equals(arg, "--daemon") && args.length == 1) {
                    isDaemon = true;
                } else {
                    log.error("command not supported: " + arg);
                    System.out.println("Not supported!");
                    return;
                }
            }

            TonieHandler tonieHandler = new TonieHandler(tonieboxProperties.getProperty("username"), tonieboxProperties.getProperty("password"));
            if (isDaemon) {
                SpotifyAuthenticationSetup.refreshToken(spotifyHandler);
                List<Pair<Tonie, PlaylistSimplified>> mappings = new ArrayList<>();
                int i = 0;
                String mapping;
                do {
                    mapping = spotifytonieboxsyncProperties.getProperty("mapping[" + i + "]");
                    log.debug("Mapping from file: " + mapping);
                    if (mapping != null) {
                        String[] pairs = mapping.split(";");
                        Tonie tonie = getTonieById(tonieHandler, pairs[0]);
                        PlaylistSimplified playlist = getPlaylistByUri(spotifyHandler, pairs[1]);
                        if (tonie != null && playlist != null) {
                            log.trace("Added Tuple");
                            mappings.add(Pair.of(tonie, playlist));
                        } else {
                            log.error("Tonie or Playlist not found");
                        }
                    }
                    i++;
                } while (mapping != null);

                while (true) {
                    for (Pair<Tonie, PlaylistSimplified> pair : mappings) {
                        sync(spotifyHandler, tonieHandler, pair.getRight(), pair.getLeft());
                    }
                    log.debug("Taking a nap");
                    Thread.sleep(60000);
                    log.debug("Up again");
                }
            } else {
                if (StringUtils.isBlank(tonieName) || StringUtils.isBlank(playlistName)) {
                    log.error("tonieName or playlistName is empty");
                    System.out.println("Not supported!");
                    return;
                }

                SpotifyAuthenticationSetup.refreshToken(spotifyHandler);
                Tonie tonie = getTonieByName(tonieHandler, tonieName);
                PlaylistSimplified playlist = getPlaylistByName(spotifyHandler, playlistName);

                if (tonie == null || playlist == null) {
                    log.error("Tonie or Playlist not found");
                    System.out.println("Tonie or Playlist not found");
                    return;
                }
                sync(spotifyHandler, tonieHandler, playlist, tonie);
            }
        } catch (IOException e) {
            log.error("IOException", e);
        } catch (SpotifyWebApiException e) {
            log.error("SpotifyWebApiException", e);
        } catch (InterruptedException e) {
            log.error("InterruptedException", e);
        }
    }

    private static void sync(SpotifyHandler spotifyHandler, TonieHandler tonieHandler, PlaylistSimplified playlist, Tonie tonie) throws IOException, SpotifyWebApiException, InterruptedException {
        List<PlaylistTrack> allTracks = PlaylistHandler.getTracks(spotifyHandler, playlist);
        TonieChapterBean[] allChapters = tonieHandler.getTonieDetails(tonie).getData().getChapters();

        List<TonieChapterBean> chaptersToRemove = PlaylistUtils.determineChaptersToRemove(allTracks, allChapters);
        deleteChapter(tonieHandler, tonie, chaptersToRemove);

        List<PlaylistTrack> trackCacheToClean = PlaylistUtils.determineCacheFilesToRenew(allChapters, allTracks);
        deleteCacheFileForTracks(spotifyHandler, trackCacheToClean);

        List<PlaylistTrack> tracks = PlaylistUtils.getDownloadList(allTracks, tonieHandler.getTonieDetails(tonie).getData().getChapters());
        startDownload(tonieHandler, spotifyHandler, tonie, tracks);

        TonieChapterBean[] sortedChapters = PlaylistUtils.getSortedChapterList(allTracks, tonieHandler.getTonieDetails(tonie).getData().getChapters());
        tonieHandler.updateChapters(tonie, sortedChapters);
    }

    private static Tonie getTonieByName(TonieHandler tonieHandler, String tonieName) throws IOException {
        List<Tonie> tonies = tonieHandler.getTonies();
        for (Tonie tonie : tonies) {
            log.trace("tonie: " + tonie);
            if (StringUtils.equals(tonie.getName(), tonieName)) {
                log.debug("tonie match: " + tonie);
                return tonie;
            }
        }
        return null;
    }

    private static Tonie getTonieById(TonieHandler tonieHandler, String tonieId) throws IOException {
        List<Tonie> tonies = tonieHandler.getTonies();
        for (Tonie tonie : tonies) {
            log.trace("tonie: " + tonie);
            if (StringUtils.equals(tonie.getTonieId(), tonieId)) {
                log.debug("tonie match: " + tonie);
                return tonie;
            }
        }
        return null;
    }

    private static PlaylistSimplified getPlaylistByName(SpotifyHandler spotifyHandler, String playlistName) throws IOException, SpotifyWebApiException {
        List<PlaylistSimplified> playlists = PlaylistHandler.getPlaylists(spotifyHandler);
        for (PlaylistSimplified playlist : playlists) {
            log.trace("Playlist name: " + playlist.getName());
            if (StringUtils.equals(playlist.getName(), playlistName)) {
                log.trace("Playlist match: " + playlistName);
                return playlist;
            }
        }
        return null;
    }

    private static PlaylistSimplified getPlaylistByUri(SpotifyHandler spotifyHandler, String playlistUri) throws IOException, SpotifyWebApiException {
        List<PlaylistSimplified> playlists = PlaylistHandler.getPlaylists(spotifyHandler);
        for (PlaylistSimplified playlist : playlists) {
            log.trace("Playlist id: " + playlist.getUri());
            if (StringUtils.equals(playlist.getUri(), playlistUri)) {
                log.trace("Playlist match: " + playlistUri);
                return playlist;
            }
        }
        return null;
    }

    private static void startDownload(TonieHandler tonieHandler, SpotifyHandler spotifyHandler, Tonie tonie, List<PlaylistTrack> tracks) throws IOException, InterruptedException {
        for (PlaylistTrack track : tracks) {
            log.debug("Track: " + track.getTrack().getId());
            String trackFilename = track.getTrack().getId() + ".mp3";
            log.trace("Track filename: " + trackFilename);
            SpotifyRecordingHandler.recordTrack(spotifyHandler, track, trackFilename);
            tonieHandler.uploadFile(tonie, getTrackTitle(track), spotifyHandler.getCachePath() + "/" + trackFilename);
        }
    }

    private static String getTrackTitle(PlaylistTrack track) {
        String trackTitle = track.getTrack().getId() + " - " + track.getTrack().getArtists()[0].getName() + " - " + track.getTrack().getName();
        log.debug(trackTitle);
        return trackTitle;
    }

    private static void deleteCacheFileForTracks(SpotifyHandler spotifyHandler, List<PlaylistTrack> tracks) {
        for (PlaylistTrack track : tracks) {
            log.debug("Deleting cached file for track: " + track.getTrack().getId());
            String trackFilename = track.getTrack().getId() + ".mp3";
            File file = new File(spotifyHandler.getCachePath() + "/" + trackFilename);
            file.delete();
        }
    }

    private static void deleteChapter(TonieHandler tonieHandler, Tonie tonie, List<TonieChapterBean> chapters) throws IOException {
        for (TonieChapterBean chapter : chapters) {
            log.debug("Deleting chapter: " + chapter.getTitle());
            tonieHandler.deleteChapter(tonie, chapter);
        }
    }

    private static Properties getProperties(String propertiesFile) throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream;
        if (new File("./" + propertiesFile).exists()) {
            stream = new FileInputStream("./" + propertiesFile);
        } else {
            stream = loader.getResourceAsStream(propertiesFile);
        }
        Properties properties = new Properties();
        properties.load(stream);
        stream.close();

        return properties;
    }
}