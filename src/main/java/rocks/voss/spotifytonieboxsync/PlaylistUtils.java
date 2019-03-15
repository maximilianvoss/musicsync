package rocks.voss.spotifytonieboxsync;

import com.wrapper.spotify.model_objects.specification.PlaylistTrack;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import rocks.voss.toniebox.beans.toniebox.Chapter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

class PlaylistUtils {
    final private static Logger log = Logger.getLogger(PlaylistUtils.class.getName());

    public static List<Chapter> determineChaptersToRemove(List<PlaylistTrack> tracks, Chapter[] chapters) {
        List<Chapter> resultList = new ArrayList<>(chapters.length);
        for (Chapter chapter : chapters) {
            log.debug("Chapter: " + chapter.getTitle());

            if (!Pattern.matches("\\w{22}\\s-\\s.+\\s-\\s.+", chapter.getTitle())) {
                log.debug("does not match");
                continue;
            }
            PlaylistTrack track = findTrackByChapter(tracks, chapter);

            if (track == null) {
                log.debug("Adding chapter for deletion: " + chapter.getTitle());
                resultList.add(chapter);
            }
            if (track != null && !isDurationValid(chapter, track)) {
                log.debug("Adding chapter for deletion: " + chapter.getTitle());
                resultList.add(chapter);
            }
        }
        return resultList;
    }

    public static List<PlaylistTrack> determineCacheFilesToRenew(Chapter[] chapters, List<PlaylistTrack> tracks) {
        List<PlaylistTrack> resultList = new ArrayList<>(chapters.length);
        for (Chapter chapter : chapters) {
            log.debug("Chapter: " + chapter.getTitle());
            PlaylistTrack track = findTrackByChapter(tracks, chapter);

            if (track != null && !isDurationValid(chapter, track)) {
                log.debug("Adding track for deletion: " + chapter.getTitle());
                resultList.add(track);
            }
        }
        return resultList;
    }

    public static Chapter[] getSortedChapterList(List<PlaylistTrack> tracks, Chapter[] chapters) {
        List<Chapter> sorted = new ArrayList<>(chapters.length);

        for (Chapter chapter : chapters) {
            if (!Pattern.matches("\\w{22}\\s-\\s.+\\s-\\s.+", chapter.getTitle())) {
                log.debug("Chapter added directly: " + chapter.getTitle());
                sorted.add(chapter);
            }
        }
        for (PlaylistTrack track : tracks) {
            log.debug("Track: " + track.getTrack().getId());
            Chapter chapter = findChapterByTrack(chapters, track);
            if ( chapter != null ) {
                log.debug("Chapter added: " + chapter.getTitle());
                sorted.add(chapter);
            }
        }
        return sorted.toArray(new Chapter[]{});
    }

    public static List<PlaylistTrack> getDownloadList(List<PlaylistTrack> tracks, Chapter[] chapters) {
        List<PlaylistTrack> resultList = new ArrayList<>(tracks.size());
        for (PlaylistTrack track : tracks) {
            log.trace("Track: " + track.getTrack().getId());
            if (findChapterByTrack(chapters, track) == null) {
                log.debug("Adding track to download list: " + track.getTrack().getId());
                resultList.add(track);
            }
        }
        return resultList;
    }

    private static PlaylistTrack findTrackByChapter(List<PlaylistTrack> tracks, Chapter chapter) {
        for (PlaylistTrack track : tracks) {
            if (StringUtils.startsWith(chapter.getTitle(), track.getTrack().getId())) {
                return track;
            }
        }
        return null;
    }


    private static Chapter findChapterByTrack(Chapter[] chapters, PlaylistTrack track) {
        for (Chapter chapter : chapters) {
            if (StringUtils.startsWith(chapter.getTitle(), track.getTrack().getId())) {
                return chapter;
            }
        }
        return null;
    }

    private static boolean isDurationValid(Chapter chapter, PlaylistTrack track) {
        float delta = chapter.getSeconds() * 1000 - track.getTrack().getDurationMs();
        log.debug("Chapter length: " + chapter.getSeconds() * 1000);
        log.debug("Track length: " + track.getTrack().getDurationMs());
        if (delta > 2000 || delta < -2000) {
            log.debug("duration is over 2s apart");
            return false;
        }
        return true;
    }
}
