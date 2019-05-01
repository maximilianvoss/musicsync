package rocks.voss.musicsync.plugins.input.spotify;

import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.tritonus.share.sampled.file.TAudioFileFormat;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public class SpotifyRecordingHandler {
    final private static Logger log = Logger.getLogger(SpotifyRecordingHandler.class.getName());

    public static void recordTrack(SpotifyHandler spotifyHandler, PlaylistTrack track, String filePath)
            throws IOException, InterruptedException {
        new File(spotifyHandler.getCachePath()).mkdirs();

        filePath = filePath.replace("//", "/");
        String filename = StringUtils.replace(filePath, spotifyHandler.getCachePath(), "");
        if (StringUtils.startsWith(filename, "/")) {
            filename = StringUtils.substring(filename, 1);
        }

        if (isFileValid(track, spotifyHandler.getCachePath(), filename)) {
            log.debug("File is valid: " + filename);
        } else {
            log.debug("File is not valid: " + filename);
            downloadFile(spotifyHandler, track, filename);
        }
    }

    private static void downloadFile(SpotifyHandler spotifyHandler, PlaylistTrack track, String filename) throws InterruptedException, IOException {
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
        Process proc = rt.exec(commands);
        proc.waitFor();
        log.debug("Execution done");
    }

    private static boolean isFileValid(PlaylistTrack track, String path, String filename) {
        log.debug("Checking: " + filename);

        File dir = new File(path);
        File[] files = dir.listFiles((directory, dirFile) -> StringUtils.equals(dirFile, filename));

        if (files != null && files.length == 1) {
            try {
                AudioFileFormat fileFormat = AudioSystem.getAudioFileFormat(files[0]);
                if (fileFormat instanceof TAudioFileFormat) {
                    Map<?, ?> properties = ((TAudioFileFormat) fileFormat).properties();
                    String key = "duration";
                    Long microseconds = (Long) properties.get(key);
                    int mili = (int) (microseconds / 1000);
                    int delta = mili - track.getTrack().getDurationMs();

                    log.debug("MP3 duration  : " + mili);
                    log.debug("Track duration: " + track.getTrack().getDurationMs());
                    log.debug("Delta duration: " + delta);

                    if (delta > 1000 || delta < -1000) {
                        log.info("Delta is too big");
                        files[0].delete();
                        return false;
                    }
                } else {
                    log.info("File is not okay");
                    files[0].delete();
                    return false;
                }
            } catch (IOException e) {
                log.error("IOException", e);
                files[0].delete();
                return false;
            } catch (UnsupportedAudioFileException e) {
                log.error("UnsupportedAudioFileException", e);
                files[0].delete();
                return false;
            }
            log.info("File is okay");
            return true;
        }
        log.info("File doesn't exists");
        return false;
    }
}
