package rocks.voss.spotifytonieboxsync;

import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.model_objects.specification.PlaylistSimplified;
import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.log4j.Logger;
import rocks.voss.toniebox.TonieHandler;
import rocks.voss.toniebox.beans.toniebox.Chapter;
import rocks.voss.toniebox.beans.toniebox.CreativeTonie;
import rocks.voss.toniebox.beans.toniebox.Household;

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
            Properties appProperties = getProperties("spotify-toniebox-sync.properties");
            SpotifyHandler spotifyHandler = SpotifyHandler.createHandlerByProperties(appProperties);

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
                    printHelp();
                    return;
                }
            }
            TonieHandler tonieHandler = new TonieHandler();
            tonieHandler.login(appProperties.getProperty("toniebox.username"), appProperties.getProperty("toniebox.password"));
            List<Household> households = tonieHandler.getHouseholds();
            List<CreativeTonie> creativeTonies = tonieHandler.getCreativeTonies(households.get(0));

            if (isDaemon) {
                daemonExecution(appProperties, spotifyHandler, tonieHandler, creativeTonies);
            } else {
                singleExecution(spotifyHandler, tonieHandler, creativeTonies, playlistName, tonieName);
            }

        } catch (IOException e) {
            log.error("IOException", e);
        } catch (SpotifyWebApiException e) {
            log.error("SpotifyWebApiException", e);
        } catch (InterruptedException e) {
            log.error("InterruptedException", e);
        }
    }

    private static void daemonExecution(Properties appProperties, SpotifyHandler spotifyHandler, TonieHandler tonieHandler, List<CreativeTonie> creativeTonies) throws IOException, SpotifyWebApiException, InterruptedException {
        List<Pair<CreativeTonie, PlaylistSimplified>> mappings = new ArrayList<>();
        int i = 0;
        String mapping;
        do {
            SpotifyAuthenticationSetup.refreshToken(spotifyHandler);
            mapping = appProperties.getProperty("mapping[" + i + "]");
            log.debug("Mapping from file: " + mapping);
            if (mapping != null) {
                String[] pairs = mapping.split(";");
                CreativeTonie tonie = getTonieById(creativeTonies, pairs[0]);
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
            for (Pair<CreativeTonie, PlaylistSimplified> pair : mappings) {
                sync(spotifyHandler, pair.getRight(), pair.getLeft());
            }
            log.debug("Taking a nap");
            Thread.sleep(60000);
            log.debug("Up again");
        }
    }

    private static void singleExecution(SpotifyHandler spotifyHandler, TonieHandler tonieHandler, List<CreativeTonie> creativeTonies, String playlistName, String tonieName) throws IOException, SpotifyWebApiException, InterruptedException {
        if (StringUtils.isBlank(tonieName) || StringUtils.isBlank(playlistName)) {
            log.error("tonieName or playlistName is empty");
            printHelp();
            return;
        }

        SpotifyAuthenticationSetup.refreshToken(spotifyHandler);
        CreativeTonie tonie = getTonieByName(creativeTonies, tonieName);
        PlaylistSimplified playlist = getPlaylistByName(spotifyHandler, playlistName);

        if (tonie == null || playlist == null) {
            log.error("Tonie or Playlist not found");
            System.out.println("Tonie or Playlist not found");
            return;
        }
        sync(spotifyHandler, playlist, tonie);
    }

    private static void printHelp() {
        StringBuffer help = new StringBuffer();
        help.append("Usage: spotify-toniebox-sync.jar ")
                .append("[--apicode | [--code CODE] | [--playlist PLAYLIST --tonie TONIE | --daemon]\n")
                .append("--apicode\t\t\t\t\t\t\tGenerate an API Code URL to get attach Application to Spotify Account\n")
                .append("--code CODE\t\t\t\t\t\tGenerate a Refresh token out of the Spotify Code\n")
                .append("--playlist PLAYLIST --tonie TONIE\tSync the PLAYLIST to tonie TONIE once\n")
                .append("--daemon\t\t\t\t\t\t\tRun in daemon mode to sync periodically all lists in the properties file\n");
        System.out.println(help.toString());
    }


    private static void sync(SpotifyHandler spotifyHandler, PlaylistSimplified
            playlist, CreativeTonie tonie) throws IOException, SpotifyWebApiException, InterruptedException {
        List<PlaylistTrack> allTracks = PlaylistHandler.getTracks(spotifyHandler, playlist);

        List<Chapter> chaptersToRemove = PlaylistUtils.determineChaptersToRemove(allTracks, tonie.getChapters());
        deleteChapter(tonie, chaptersToRemove);

        List<PlaylistTrack> trackCacheToClean = PlaylistUtils.determineCacheFilesToRenew(tonie.getChapters(), allTracks);
        deleteCacheFileForTracks(spotifyHandler, trackCacheToClean);

        List<PlaylistTrack> tracks = PlaylistUtils.getDownloadList(allTracks, tonie.getChapters());
        startDownload(spotifyHandler, tonie, tracks);

        tonie.setChapters(PlaylistUtils.getSortedChapterList(allTracks, tonie.getChapters()));
        tonie.commit();
    }

    private static CreativeTonie getTonieByName(List<CreativeTonie> creativeTonies, String tonieName) throws IOException {
        for (CreativeTonie tonie : creativeTonies) {
            log.trace("tonie: " + tonie);
            if (StringUtils.equals(tonie.getName(), tonieName)) {
                log.debug("tonie match: " + tonie);
                return tonie;
            }
        }
        return null;
    }

    private static CreativeTonie getTonieById(List<CreativeTonie> creativeTonies, String tonieId) throws IOException {
        for (CreativeTonie tonie : creativeTonies) {
            log.trace("tonie: " + tonie);
            if (StringUtils.equals(tonie.getId(), tonieId)) {
                log.debug("tonie match: " + tonie);
                return tonie;
            }
        }
        return null;
    }

    private static PlaylistSimplified getPlaylistByName(SpotifyHandler spotifyHandler, String playlistName) throws
            IOException, SpotifyWebApiException {
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

    private static PlaylistSimplified getPlaylistByUri(SpotifyHandler spotifyHandler, String playlistUri) throws
            IOException, SpotifyWebApiException {
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

    private static void startDownload(SpotifyHandler spotifyHandler, CreativeTonie
            tonie, List<PlaylistTrack> tracks) throws IOException, InterruptedException {
        for (PlaylistTrack track : tracks) {
            log.debug("Track: " + track.getTrack().getId());
            String trackFilename = track.getTrack().getId() + ".mp3";
            log.trace("Track filename: " + trackFilename);
            SpotifyRecordingHandler.recordTrack(spotifyHandler, track, trackFilename);
            tonie.uploadFile(getTrackTitle(track), spotifyHandler.getCachePath() + "/" + trackFilename);
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

    private static void deleteChapter(CreativeTonie tonie, List<Chapter> chapters) throws IOException {
        for (Chapter chapter : chapters) {
            log.debug("Deleting chapter: " + chapter.getTitle());
            tonie.deleteChapter(chapter);
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