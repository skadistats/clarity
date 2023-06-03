package skadistats.clarity.processor.stringtables;

import com.google.protobuf.ByteString;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.Insert;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.model.csgo.PlayerInfoType;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

@Provides(value = {UsesPlayerInfo.class, OnPlayerInfo.class}, engine = { EngineId.CSGO_S1, EngineId.CSGO_S2})
@UsesStringTable("userinfo")
public class PlayerInfo {

    @Insert
    private EngineType engineType;

    @InsertEvent
    private Event<OnPlayerInfo> evPlayerInfo;

    private final Map<Integer, PlayerInfoType> playerInfos = new TreeMap<>();

    @OnStringTableEntry("userinfo")
    public void onEntry(StringTable table, int index, String key, ByteString value) {
        PlayerInfoType current = null;
        if (value != null && value.size() != 0) {
            switch (engineType.getId()) {
                case CSGO_S1:
                    current = PlayerInfoType.createS1(value);
                    break;
                case CSGO_S2:
                    current = PlayerInfoType.createS2(value);
                    break;
                default:
                    throw new UnsupportedOperationException("don't know how to handle player info");
            }
        }

        var i = index + 1;
        var last = playerInfos.get(i);

        if (current == last) return;
        if (current != null && current.equals(last)) return;

        if (current == null) {
            playerInfos.remove(i);
        } else {
            playerInfos.put(i, current);
        }
        evPlayerInfo.raise(i, current);
    }

    public PlayerInfoType getPlayerInfoForEntityIndex(int i) {
        return playerInfos.get(i);
    }

    public Integer getEntityIndexForUserId(final int userId) {
        for (var e : playerInfos.entrySet()) {
            if (e.getValue().getUserId() == userId) {
                return e.getKey();
            }
        }
        return null;
    }

}
