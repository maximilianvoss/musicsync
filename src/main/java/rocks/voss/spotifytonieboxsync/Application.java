package rocks.voss.spotifytonieboxsync;

import com.wrapper.spotify.exceptions.SpotifyWebApiException;
import com.wrapper.spotify.exceptions.detailed.ServiceUnavailableException;
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

class Application {
    final private static Logger log = Logger.getLogger(Application.class.getName());
    final private static String PROPERTIES_FILE = "spotify-toniebox-sync.properties";
    private static Properties appProperties;

    public static void main(String[] args) {
        try {
            appProperties = getProperties();
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
                daemonExecution(spotifyHandler, creativeTonies);
            } else {
                singleExecution(spotifyHandler, creativeTonies, playlistName, tonieName);
            }
        } catch (IOException e) {
            log.error("IOException", e);
        } catch (SpotifyWebApiException e) {
            log.error("SpotifyWebApiException", e);
        } catch (InterruptedException e) {
            log.error("InterruptedException", e);
        }
    }

    private static void daemonExecution(SpotifyHandler spotifyHandler, List<CreativeTonie> creativeTonies)
            throws IOException, SpotifyWebApiException, InterruptedException {
        List<Pair<CreativeTonie, PlaylistSimplified>> mappings = new ArrayList<>();
        int i = 0;
        String mapping;
        SpotifyAuthenticationSetup.refreshToken(spotifyHandler);
        do {
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
            SpotifyAuthenticationSetup.refreshToken(spotifyHandler);
            for (Pair<CreativeTonie, PlaylistSimplified> pair : mappings) {
                try {
                    sync(spotifyHandler, pair.getRight(), pair.getLeft());
                } catch (ServiceUnavailableException e) {
                    log.error("ServiceUnavailableException during sync execution: ", e);
                } catch (SpotifyWebApiException e) {
                    log.error("SpotifyWebApiException during sync execution: ", e);
                } catch (IOException e) {
                    log.error("IOException during sync execution: ", e);
                } catch (InterruptedException e) {
                    log.error("InterruptedException during sync execution: ", e);
                }
            }
            log.debug("Taking a nap");
            Thread.sleep(60000);
            log.debug("Up again");
        }
    }

    private static void singleExecution(SpotifyHandler spotifyHandler, List<CreativeTonie> creativeTonies, String playlistName, String tonieName)
            throws IOException, SpotifyWebApiException, InterruptedException {
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
        String help = "Usage: spotify-toniebox-sync.jar " +
                "[--apicode | [--code CODE] | [--playlist PLAYLIST --tonie TONIE | --daemon]\n" +
                "--apicode\t\t\t\t\t\t\tGenerate an API Code URL to get attach Application to Spotify Account\n" +
                "--code CODE\t\t\t\t\t\tGenerate a Refresh token out of the Spotify Code\n" +
                "--playlist PLAYLIST --tonie TONIE\tSync the PLAYLIST to tonie TONIE once\n" +
                "--daemon\t\t\t\t\t\t\tRun in daemon mode to sync periodically all lists in the properties file\n";
        System.out.println(help);
    }


    private static void sync(SpotifyHandler spotifyHandler, PlaylistSimplified playlist, CreativeTonie tonie)
            throws IOException, SpotifyWebApiException, InterruptedException {
        List<PlaylistTrack> allTracks = PlaylistHandler.getTracks(spotifyHandler, playlist);

        List<Chapter> chaptersToRemove = PlaylistUtils.determineChaptersToRemove(allTracks, tonie.getChapters());
        deleteChapter(tonie, chaptersToRemove);

        List<PlaylistTrack> trackCacheToClean = PlaylistUtils.determineCacheFilesToRenew(tonie.getChapters(), allTracks);
        deleteCacheFileForTracks(spotifyHandler, trackCacheToClean);

        List<PlaylistTrack> tracks = PlaylistUtils.getDownloadList(allTracks, tonie.getChapters());
        transferFromSpotifyToToniebox(spotifyHandler, tonie, tracks);

        tonie.setChapters(PlaylistUtils.getSortedChapterList(allTracks, tonie.getChapters()));
        tonie.commit();
    }

    private static CreativeTonie getTonieByName(List<CreativeTonie> creativeTonies, String tonieName) {
        for (CreativeTonie tonie : creativeTonies) {
            log.trace("tonie: " + tonie);
            if (StringUtils.equals(tonie.getName(), tonieName)) {
                log.debug("tonie match: " + tonie);
                return tonie;
            }
        }
        return null;
    }

    private static CreativeTonie getTonieById(List<CreativeTonie> creativeTonies, String tonieId) {
        for (CreativeTonie tonie : creativeTonies) {
            log.trace("tonie: " + tonie);
            if (StringUtils.equals(tonie.getId(), tonieId)) {
                log.debug("tonie match: " + tonie);
                return tonie;
            }
        }
        return null;
    }

    private static PlaylistSimplified getPlaylistByName(SpotifyHandler spotifyHandler, String playlistName)
            throws IOException, SpotifyWebApiException {
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

    private static PlaylistSimplified getPlaylistByUri(SpotifyHandler spotifyHandler, String playlistUri)
            throws IOException, SpotifyWebApiException {
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

    private static void transferFromSpotifyToToniebox(SpotifyHandler spotifyHandler, CreativeTonie tonie, List<PlaylistTrack> tracks)
            throws IOException, InterruptedException {
        for (PlaylistTrack track : tracks) {
            log.debug("Track: " + track.getTrack().getId());
            String trackFilename = track.getTrack().getId() + ".mp3";
            log.trace("Track filename: " + trackFilename);
            SpotifyRecordingHandler.recordTrack(spotifyHandler, track, trackFilename);
            tonie.uploadFile(getTrackTitle(track), spotifyHandler.getCachePath() + "/" + trackFilename);
        }
    }

    private static String getTrackTitle(PlaylistTrack track) {
        String trackTitle = appProperties.getProperty("toniebox.trackname");
        if ( StringUtils.isBlank(trackTitle)) {
            trackTitle = "%d - %a - %n";
        }
        trackTitle = StringUtils.replace(trackTitle, "%i", track.getTrack().getId());
        trackTitle = StringUtils.replace(trackTitle, "%a", track.getTrack().getArtists()[0].getName());
        trackTitle = StringUtils.replace(trackTitle, "%n", track.getTrack().getName());
        trackTitle = StringUtils.replace(trackTitle, "%d", String.valueOf(track.getTrack().getDiscNumber()));
        trackTitle = StringUtils.replace(trackTitle, "%t", String.valueOf(track.getTrack().getTrackNumber()));

        /*The maximum chapter length is 128 characters. Note that this might result in the unlikely failure
        to delete the track automatically if the second "-" character is cut away.*/
        trackTitle = StringUtils.left(trackTitle, 128);

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

    private static Properties getProperties() throws IOException {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        InputStream stream;
        if (new File("./" + PROPERTIES_FILE).exists()) {
            stream = new FileInputStream("./" + PROPERTIES_FILE);
        } else {
            stream = loader.getResourceAsStream(PROPERTIES_FILE);
        }
        Properties properties = new Properties();
        properties.load(stream);
        if (stream != null) {
            stream.close();
        }

        return properties;
    }
}