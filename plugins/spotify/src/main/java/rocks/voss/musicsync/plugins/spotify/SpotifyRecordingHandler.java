package rocks.voss.musicsync.plugins.spotify;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rocks.voss.musicsync.plugins.spotify.config.PluginConfiguration;
import se.michaelthelin.spotify.model_objects.specification.PlaylistTrack;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class SpotifyRecordingHandler {
    final private static Logger log = LogManager.getLogger(SpotifyRecordingHandler.class);

    public static boolean recordTrack(PluginConfiguration spotifyHandler, PlaylistTrack track, File fileDestination)
            throws IOException, InterruptedException {

        new File(fileDestination.getPath()).mkdirs();

        if (track.getTrack() == null) {
            log.debug("Track is not available on Spotify");
            return false;
        } else if (isFileValid(spotifyHandler, track, fileDestination)) {
            log.debug("File is valid: " + fileDestination.getAbsolutePath());
            return false;
        } else {
            log.debug("File is not valid: " + fileDestination.getAbsolutePath());
            downloadFile(spotifyHandler, track, fileDestination.getAbsolutePath());
            return true;
        }
    }

    private static void downloadFile(PluginConfiguration spotifyHandler, PlaylistTrack track, String filename) throws InterruptedException, IOException {
        log.info("Downloading: " + track.getTrack().getUri());
        Runtime rt = Runtime.getRuntime();
        StringBuilder command = new StringBuilder();
        command.append("stream_recorder.pl --uri '")
                .append(track.getTrack().getUri())
                .append("' --silent --format mp3 --outdir ")
                .append(spotifyHandler.getCachePath())
                .append(" --filename ")
                .append(filename);

        String[] commands = {"/bin/bash", "-c", command.toString()};
        log.debug("Executing: " + command.toString());
        rt.exec(commands).waitFor();
        log.debug("Execution done");
    }

    public static boolean isFileValid(PluginConfiguration spotifyHandler, PlaylistTrack track, File file) {
        log.debug("Checking: " + file.getAbsolutePath());

        if (file.exists() && file.isFile() && !file.isDirectory()) {
            try {
                AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(file);
                Map<?, ?> properties = fileFormat.properties();
                String key = "duration";
                Long microseconds = (Long) properties.get(key);
                int mili = (int) (microseconds / 1000);
                int delta = mili - track.getTrack().getDurationMs();

                log.info("MP3 duration  : " + mili);
                log.info("Track duration: " + track.getTrack().getDurationMs());
                log.info("Delta duration: " + delta);

                if (delta > spotifyHandler.getTrackThreshold() || delta < -spotifyHandler.getTrackThreshold()) {
                    log.info("Delta is too big");
                    file.delete();
                    return false;
                }
            } catch (IOException | UnsupportedAudioFileException e) {
                log.error("Exception in file: " + file.getAbsolutePath(), e);
                file.delete();
                return false;
            }
            log.info("File is okay: " + file.getAbsolutePath());
            return true;
        }
        log.info("File doesn't exists: " + file.getAbsolutePath());
        return false;
    }
}
