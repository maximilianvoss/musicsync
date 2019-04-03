package rocks.voss.musicsync.plugins.output.toniebox;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import rocks.voss.musicsync.api.SyncConnection;

@Data
public class TonieboxConnection {
    private String householdId;
    private String creativeTonieId;

    public static TonieboxConnection createBy(SyncConnection connection) {
        TonieboxConnection tonieboxConnection = new TonieboxConnection();
        String pairs[] = StringUtils.split(connection.getOutputUri(), ':');
        tonieboxConnection.setHouseholdId(pairs[1]);
        tonieboxConnection.setCreativeTonieId(pairs[2]);
        return  tonieboxConnection;
    }
}
