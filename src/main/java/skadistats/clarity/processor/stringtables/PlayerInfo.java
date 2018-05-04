package skadistats.clarity.processor.stringtables;

import com.google.protobuf.ByteString;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.EngineId;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.model.csgo.PlayerInfoType;

import java.io.IOException;
import java.util.Map;
import java.util.TreeMap;

@Provides(value = {UsesPlayerInfo.class, OnPlayerInfo.class}, engine = { EngineId.CSGO })
@UsesStringTable("userinfo")
public class PlayerInfo {

    @InsertEvent
    private Event<OnPlayerInfo> evPlayerInfo;

    private Map<Integer, PlayerInfoType> playerInfos = new TreeMap<>();

    @OnStringTableEntry("userinfo")
    public void onEntry(StringTable table, int index, String key, ByteString value) throws IOException {
        PlayerInfoType current = null;
        if (value != null && value.size() != 0) {
            current = new PlayerInfoType(value);
        }

        int i = index + 1;
        PlayerInfoType last = playerInfos.get(i);

        if (current == last) return;
        if (current != null && last != null && current.equals(last)) return;

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

}
