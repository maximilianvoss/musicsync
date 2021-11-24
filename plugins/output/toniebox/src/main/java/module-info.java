open module rocks.voss.musicsync.plugins.output.toniebox {
    requires rocks.voss.musicsync.api;
    requires lombok;
    requires org.apache.commons.lang3;
    requires log4j;
    requires static rocks.voss.toniebox.api;

    uses rocks.voss.musicsync.api.SyncOutputPlugin;
    uses rocks.voss.musicsync.api.SyncTrack;
    uses rocks.voss.musicsync.api.SyncConnection;
}
