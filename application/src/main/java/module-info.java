module rocks.voss.musicsync.application {
    requires rocks.voss.musicsync.api;
    requires lombok;
    requires org.apache.commons.lang3;
    requires org.apache.logging.log4j;
    requires org.apache.logging.log4j.core;
    requires org.reflections;
    requires rocks.voss.jsonhelper;

    exports rocks.voss.musicsync.application;
    exports rocks.voss.musicsync.application.config;

    uses rocks.voss.musicsync.api.SyncInputPlugin;
    uses rocks.voss.musicsync.api.SyncOutputPlugin;
    uses rocks.voss.musicsync.api.SyncTrack;
    uses rocks.voss.musicsync.api.SyncConnection;
}

