package rocks.voss.musicsync.api;

public interface SyncPlugin {
    /**
     * establishes a connection for the plugin to its endpoints
     */
    void establishConnection();

    /**
     * initializing the plugin
     *
     * @param configuration to initalize plugin
     * @throws Exception if something goes wrong
     */
    void init(Object configuration) throws Exception;

    /**
     * parses input arguments from the command line up on start. Can be used to set parameters which are not set in the config file
     *
     * @param args command line arguments
     * @return true = all good; false on failure
     */
    boolean parseArguments(String[] args);

    /**
     * Get the Schema of the component
     *
     * @return schema
     */
    String getSchema();

    /**
     * Close the connection of the plugin's endpoint
     */
    void closeConnection();

    /**
     * HelpScreen if arguments are insert faulty or --help is a commandl-ine parameter
     *
     * @return input for the help screen
     */
    String helpScreen();
}
