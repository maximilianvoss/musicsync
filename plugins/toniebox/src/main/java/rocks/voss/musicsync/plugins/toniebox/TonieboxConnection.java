package rocks.voss.musicsync.plugins.toniebox;

import lombok.Data;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import rocks.voss.jsonhelper.JSONHelper;
import rocks.voss.musicsync.api.SyncConnection;
import rocks.voss.musicsync.plugins.toniebox.config.SyncConfiguration;

import java.io.IOException;

@Data
public class TonieboxConnection {
    final private static Logger log = LogManager.getLogger(TonieboxConnection.class);
    private String householdId;
    private String creativeTonieId;

    public static TonieboxConnection createBy(SyncConnection connection) {
        TonieboxConnection tonieboxConnection = new TonieboxConnection();

        try {
            SyncConfiguration config = JSONHelper.createBean(SyncConfiguration.class, connection.getOutputConfig());
            tonieboxConnection.setHouseholdId(config.getHousehold());
            tonieboxConnection.setCreativeTonieId(config.getTonie());
        } catch (IOException e) {
            log.error(e);
            return null;
        }
        return tonieboxConnection;
    }
}
