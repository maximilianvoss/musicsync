package rocks.voss.musicsync.plugins.toniebox;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rocks.voss.jsonhelper.JSONHelper;
import rocks.voss.musicsync.api.SyncConnection;
import rocks.voss.musicsync.api.SyncOutputPlugin;
import rocks.voss.musicsync.api.SyncTrack;
import rocks.voss.musicsync.plugins.toniebox.config.PluginConfiguration;
import rocks.voss.toniebox.TonieHandler;
import rocks.voss.toniebox.beans.toniebox.Chapter;
import rocks.voss.toniebox.beans.toniebox.CreativeTonie;
import rocks.voss.toniebox.beans.toniebox.Household;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

public class TonieboxOutputPlugin implements SyncOutputPlugin {
    final private static Logger log = LogManager.getLogger(TonieboxOutputPlugin.class);

    private List<Household> households;
    private TonieHandler tonieHandler;
    private Map<SyncConnection, CreativeTonie> tonieCache = new HashMap<>();
    private PluginConfiguration pluginConfiguration;

    @Override
    public String helpScreen() {
        return StringUtils.EMPTY;
    }

    @Override
    public void uploadTracks(SyncConnection connection, List<SyncTrack> syncTracks) {
        for (SyncTrack syncTrack : syncTracks) {
            uploadTrack(connection, syncTrack);
        }
    }

    @Override
    public void uploadTrack(SyncConnection connection, SyncTrack syncTrack) {
        try {
            CreativeTonie creativeTonie = getCreativeTonie(connection);
            if (creativeTonie == null) {
                log.error("CreativeTonie not found");
                return;
            }
            File f = new File(syncTrack.getPath());
            if (!f.exists()) {
                log.info("File does not exist: {}", f.getAbsolutePath());
            }
            creativeTonie.uploadFile(getTrackTitle(syncTrack), f.getAbsolutePath());
            creativeTonie.commit();

        } catch (Exception e) {
            log.error("Exception", e);
        }
    }

    @Override
    public boolean isTrackUploaded(SyncConnection connection, SyncTrack syncTrack) {
        try {
            CreativeTonie creativeTonie = getCreativeTonie(connection);
            if (creativeTonie == null) {
                log.debug("CreativeTonie not found");
                return false;
            }
            for (Chapter chapter : creativeTonie.getChapters()) {
                if (StringUtils.equals(chapter.getTitle(), getTrackTitle(syncTrack))) {
                    log.debug("Track found: " + chapter.getTitle());
                    return isValidTrack(syncTrack, chapter);
                }
            }
        } catch (Exception e) {
            log.error("Exception", e);
        }
        log.debug("Track not found: " + getTrackTitle(syncTrack));
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
        } catch (Exception e) {
            log.error("Exception", e);
        }
    }

    @Override
    public void cleanUpTracks(SyncConnection connection, List<SyncTrack> syncTracks) {
        try {
            CreativeTonie creativeTonie = getCreativeTonie(connection);
            if (creativeTonie == null) {
                log.debug("CreativeTonie not found");
                return;
            }

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
        } catch (Exception e) {
            log.error("Exception", e);
        }
    }

    @Override
    public void establishConnection() {
        openConnection();
    }

    @Override
    public void init(Object configuration) {
        try {
            pluginConfiguration = JSONHelper.createBean(PluginConfiguration.class, configuration);
        } catch (IOException e) {
            log.error(e);
        }
    }

    @Override
    public boolean parseArguments(String[] args) {
        return true;
    }

    @Override
    public String getSchema() {
        return "toniebox";
    }

    @Override
    public void closeConnection() {
        try {
            tonieHandler.disconnect();
        } catch (Exception e) {
            log.error("Exception while closing Connection", e);
        }
        tonieHandler = null;
    }

    private void openConnection() {
        try {
            if (tonieHandler == null) {
                tonieHandler = new TonieHandler();
            }
            tonieHandler.login(pluginConfiguration.getUsername(), pluginConfiguration.getPassword());
            households = tonieHandler.getHouseholds();
        } catch (Exception e) {
            log.error("Exception while login", e);
            households = new ArrayList<>(0);
            tonieCache.clear();
        }
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
            } catch (Exception e) {
                log.error("Exception", e);
            }
        }
        return tonieCache.get(syncConnection);
    }

    private String getTrackTitle(SyncTrack syncTrack) {
        return StringUtils.left(syncTrack.getId() + " - " + syncTrack.getArtists()[0] + " - " + syncTrack.getName(), 128);
    }

    private boolean isChapterToBeRemoved(List<SyncTrack> syncTracks, Chapter chapter) {
        if (!Pattern.matches("\\w{22}\\s-\\s.+\\s-\\s.+", chapter.getTitle())) {
            return false;
        }
        for (SyncTrack syncTrack : syncTracks) {
            if (StringUtils.equals(chapter.getTitle(), getTrackTitle(syncTrack))) {
                log.debug("Chapter found: " + chapter.getTitle());
                if (syncTrack.isFresh()) {
                    log.debug("Track was updated and needs reupload: ", syncTrack.getId());
                    return true;
                }
                if (isValidTrack(syncTrack, chapter)) {
                    log.debug("discrepancy between source & target: ", syncTrack.getId());
                    return true;
                }
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

    private boolean isValidTrack(SyncTrack syncTrack, Chapter chapter) {
        int chapterDuration = Math.round(chapter.getSeconds());
        int diff = chapterDuration - syncTrack.getTrackDuration();

        log.debug("SyncTrack length: ", syncTrack.getTrackDuration());
        log.debug("Chapter length: ", chapter.getSeconds());
        log.debug("Calc'd Diff: ", diff);

        if (diff > pluginConfiguration.getThreshold() || diff < -pluginConfiguration.getThreshold()) {
            log.debug("not valid");
            return false;
        }
        log.debug("valid");
        return true;
    }
}
