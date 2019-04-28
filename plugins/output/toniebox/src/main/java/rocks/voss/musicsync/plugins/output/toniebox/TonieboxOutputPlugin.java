package rocks.voss.musicsync.plugins.output.toniebox;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import rocks.voss.musicsync.api.SyncConnection;
import rocks.voss.musicsync.api.SyncOutputPlugin;
import rocks.voss.musicsync.api.SyncTrack;
import rocks.voss.toniebox.TonieHandler;
import rocks.voss.toniebox.beans.toniebox.Chapter;
import rocks.voss.toniebox.beans.toniebox.CreativeTonie;
import rocks.voss.toniebox.beans.toniebox.Household;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Pattern;

public class TonieboxOutputPlugin implements SyncOutputPlugin {
    final private Logger log = Logger.getLogger(this.getClass().getName());
    private List<Household> households;
    private TonieHandler tonieHandler = new TonieHandler();
    private Map<SyncConnection, CreativeTonie> tonieCache = new HashMap<>();
    private String username;
    private String password;

    @Override
    public String helpScreen() {
        return "--toniebox-username USERNAME\n\t\tSet the Toniebox username to login to Toniebox\n" +
                "--toniebox-password PASSWORD\n\t\tSet the password for the Toniebox user for login\n";
    }

    @Override
    public void uploadTracks(SyncConnection connection, List<SyncTrack> syncTracks) {
        CreativeTonie creativeTonie = getCreativeTonie(connection);
        if (creativeTonie == null) {
            log.debug("CreativeTonie not found");
            return;
        }
        try {
            for (SyncTrack syncTrack : syncTracks) {
                creativeTonie.uploadFile(getTrackTitle(syncTrack), syncTrack.getCacheLocation());
            }
            creativeTonie.commit();
        } catch (IOException e) {
            log.error("IOException", e);
        }
    }

    @Override
    public boolean isTrackUploaded(SyncConnection connection, SyncTrack syncTrack) {
        CreativeTonie creativeTonie = getCreativeTonie(connection);
        if (creativeTonie == null) {
            log.debug("CreativeTonie not found");
            return false;
        }
        for (Chapter chapter : creativeTonie.getChapters()) {
            if (StringUtils.equals(chapter.getTitle(), getTrackTitle(syncTrack))) {
                log.debug("Track found: " + chapter.getTitle());
                return true;
            }
        }
        return false;
    }

    @Override
    public void orderTracks(SyncConnection connection, List<SyncTrack> syncTracks) {
        CreativeTonie creativeTonie = getCreativeTonie(connection);
        if (creativeTonie == null) {
            log.debug("CreativeTonie not found");
            return;
        }

        try {
            List<Chapter> sortedChapters = new ArrayList<>(creativeTonie.getChapters().length);
            log.debug("Put unknown tracks to front");
            for (Chapter chapter : creativeTonie.getChapters()) {
                if (!Pattern.matches("\\w{22}\\s-\\s.+\\s-\\s.+", chapter.getTitle())) {
                    sortedChapters.add(chapter);
                }
            }
            log.debug("Sorting known tracks");
            for (SyncTrack syncTrack : syncTracks) {
                Chapter chapter = findChapter(creativeTonie.getChapters(), syncTrack);
                if (chapter != null) {
                    sortedChapters.add(chapter);
                }
            }

            creativeTonie.setChapters(sortedChapters.toArray(new Chapter[]{}));
            creativeTonie.commit();
        } catch (IOException e) {
            log.error("IOException", e);
        }
    }

    @Override
    public void cleanUpTracks(SyncConnection connection, List<SyncTrack> syncTracks) {
        CreativeTonie creativeTonie = getCreativeTonie(connection);
        if (creativeTonie == null) {
            log.debug("CreativeTonie not found");
            return;
        }

        try {
            List<Chapter> newChapters = new ArrayList<>(creativeTonie.getChapters().length);
            for (Chapter chapter : creativeTonie.getChapters()) {
                log.debug("Chapter: " + chapter.getTitle());
                if (!isChapterToBeRemoved(syncTracks, chapter)) {
                    log.debug("Chapter: " + chapter.getTitle() + " not to be deleted");
                    newChapters.add(chapter);
                }
            }

            creativeTonie.setChapters(newChapters.toArray(new Chapter[]{}));
            creativeTonie.commit();
        } catch (IOException e) {
            log.error("IOException", e);
        }
    }

    @Override
    public void init(Properties properties) throws Exception {
        username = properties.getProperty("toniebox.username");
        password = properties.getProperty("toniebox.password");
    }

    @Override
    public boolean parseArguments(String[] args) throws Exception {
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (StringUtils.equals(arg, "--toniebox-username")) {
                username = args[++i];
            } else if (StringUtils.equals(arg, "--toniebox-password")) {
                password = args[++i];
            }
        }
        tonieHandler.login(username, password);
        households = tonieHandler.getHouseholds();
        return true;
    }

    @Override
    public String getSchema() {
        return "toniebox";
    }

    private CreativeTonie getCreativeTonie(SyncConnection syncConnection) {
        if (!tonieCache.containsKey(syncConnection)) {
            log.debug("creative tonie not in cache");
            try {
                TonieboxConnection connection = TonieboxConnection.createBy(syncConnection);
                for (Household household : households) {
                    if (StringUtils.equals(household.getId(), connection.getHouseholdId())) {
                        log.debug("household found");
                        List<CreativeTonie> creativeTonies = tonieHandler.getCreativeTonies(household);
                        for (CreativeTonie creativeTonie : creativeTonies) {
                            if (StringUtils.equals(creativeTonie.getId(), connection.getCreativeTonieId())) {
                                log.debug("creative tonie found");
                                tonieCache.put(syncConnection, creativeTonie);
                                return creativeTonie;
                            }
                        }
                        return null;
                    }
                }
            } catch (IOException e) {
                log.error("IOException", e);
            }
        }
        return tonieCache.get(syncConnection);
    }

    private String getTrackTitle(SyncTrack syncTrack) {
        return syncTrack.getId() + " - " + syncTrack.getArtists()[0] + " - " + syncTrack.getName();
    }

    private boolean isChapterToBeRemoved(List<SyncTrack> syncTracks, Chapter chapter) {
        if (!Pattern.matches("\\w{22}\\s-\\s.+\\s-\\s.+", chapter.getTitle())) {
            return false;
        }
        for (SyncTrack syncTrack : syncTracks) {
            if (StringUtils.equals(chapter.getTitle(), getTrackTitle(syncTrack))) {
                log.debug("Chapter found: " + chapter.getTitle());
                return false;
            }
        }
        return true;
    }

    private Chapter findChapter(Chapter[] chapters, SyncTrack syncTrack) {
        for (Chapter chapter : chapters) {
            if (StringUtils.equals(chapter.getTitle(), getTrackTitle(syncTrack))) {
                log.debug("Chapter found: " + chapter.getTitle());
                return chapter;
            }
        }
        return null;
    }
}
