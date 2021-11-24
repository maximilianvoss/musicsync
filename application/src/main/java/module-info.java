module rocks.voss.musicsync.application {
    requires transitive rocks.voss.musicsync.api;
    requires transitive lombok;
    requires transitive org.apache.commons.lang3;
    requires transitive log4j;
    requires transitive org.reflections;

    exports rocks.voss.musicsync.application;

    uses rocks.voss.musicsync.api.SyncInputPlugin;
    uses rocks.voss.musicsync.api.SyncOutputPlugin;
    uses rocks.voss.musicsync.api.SyncTrack;
    uses rocks.voss.musicsync.api.SyncConnection;
}

