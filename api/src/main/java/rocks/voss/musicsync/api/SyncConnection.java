package rocks.voss.musicsync.api;

public interface SyncConnection {

    /**
     * Input schema for the connection which will be used to determine the Sync Input Plugin
     *
     * @return String which describes the schema
     */
    String getInputSchema();

    /**
     * Getting the Sync Plugin for the Audio input
     *
     * @return SyncPlugin
     */
    SyncInputPlugin getSyncInputPlugin();

    /**
     * Configuration which describes the parameter for the input plugin
     *
     * @return Object which needs to be casted for the SyncInputPlugin
     */
    Object getInputConfig();

    /**
     * Input schema for the connection which will be used to determine the Sync Output Plugin
     *
     * @return String which describes the schema
     */
    String getOutputSchema();

    /**
     * Getting the Sync Plugin for the Audio output
     *
     * @return SyncPlugin
     */
    SyncOutputPlugin getSyncOutputPlugin();

    /**
     * Configuration which describes the parameter for the output plugin
     *
     * @return Object which needs to be casted for the SyncOutputPlugin
     */
    Object getOutputConfig();

    /**
     * Describing the connection by a meaningful name
     *
     * @return name of the connection
     */
    String getName();
}
