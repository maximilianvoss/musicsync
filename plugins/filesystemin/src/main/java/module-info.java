open module rocks.voss.musicsync.plugins.filesystemin {
    requires rocks.voss.musicsync.api;
    requires lombok;
    requires org.apache.commons.lang3;
    requires org.apache.logging.log4j;
    requires rocks.voss.jsonhelper;

    uses rocks.voss.musicsync.api.SyncInputPlugin;
    uses rocks.voss.musicsync.api.SyncTrack;
    uses rocks.voss.musicsync.api.SyncConnection;
}
