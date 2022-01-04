open module rocks.voss.musicsync.plugins.spotify {
    requires rocks.voss.musicsync.api;
    requires lombok;
    requires org.apache.commons.lang3;
    requires org.apache.logging.log4j;
    requires rocks.voss.jsonhelper;

    requires static se.michaelthelin.spotify;
    requires static com.fasterxml.jackson.databind;
    requires static com.google.gson;
    requires static nv.i18n;
    requires static org.apache.httpcomponents.client5.httpclient5.cache;
    requires static org.apache.httpcomponents.client5.httpclient5;
    requires static org.apache.httpcomponents.core5.httpcore5;
    requires static java.desktop;

    uses rocks.voss.musicsync.api.SyncInputPlugin;
    uses rocks.voss.musicsync.api.SyncTrack;
    uses rocks.voss.musicsync.api.SyncConnection;

    exports rocks.voss.musicsync.plugins.spotify;
    exports rocks.voss.musicsync.plugins.spotify.config;
}
