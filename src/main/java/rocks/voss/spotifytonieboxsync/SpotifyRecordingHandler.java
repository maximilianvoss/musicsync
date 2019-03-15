package rocks.voss.spotifytonieboxsync;

import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

class SpotifyRecordingHandler {
    final private static Logger log = Logger.getLogger(SpotifyRecordingHandler.class.getName());

    public static void recordTrack(SpotifyHandler spotifyHandler, PlaylistTrack track, String filename)
            throws IOException, InterruptedException {
        new File(spotifyHandler.getCachePath()).mkdirs();
        File dir = new File(spotifyHandler.getCachePath());
        File[] files = dir.listFiles((directory, dirFile) -> StringUtils.equals(dirFile, filename));

        if (files != null && files.length == 0) {
            log.debug("File does not exist: " + filename);
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
        } else {
            log.debug("File does exist: " + filename);
        }
    }
}
