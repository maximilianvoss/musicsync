open module rocks.voss.musicsync.plugins.toniebox {
    requires static rocks.voss.musicsync.api;
    requires rocks.voss.toniebox.api;
    requires lombok;
    requires org.apache.commons.lang3;
    requires org.apache.logging.log4j;
    requires rocks.voss.jsonhelper;

    uses rocks.voss.musicsync.api.SyncOutputPlugin;
    uses rocks.voss.musicsync.api.SyncTrack;
    uses rocks.voss.musicsync.api.SyncConnection;
}
