package clarity.parser.handler;

import java.util.HashMap;
import java.util.Map;

import clarity.match.Match;

import com.dota2.proto.Demo.CDemoClassInfo;
import com.dota2.proto.Demo.CDemoStringTables;
import com.dota2.proto.Demo.CDemoSyncTick;
import com.dota2.proto.Netmessages.CSVCMsg_CreateStringTable;
import com.dota2.proto.Netmessages.CSVCMsg_PacketEntities;
import com.dota2.proto.Netmessages.CSVCMsg_SendTable;
import com.dota2.proto.Netmessages.CSVCMsg_UpdateStringTable;

public class HandlerRegistry {

    private static final Map<Class<?>, Handler<?>> H;
    static {
        H = new HashMap<Class<?>, Handler<?>>();

        // for prologue
        H.put(CDemoClassInfo.class, new DemClassInfoHandler());
        H.put(CDemoStringTables.class, new DemStringTablesHandler());
        H.put(CSVCMsg_CreateStringTable.class, new SvcCreateStringTableHandler());
        H.put(CSVCMsg_SendTable.class, new SvcSendTableHandler());
        H.put(CDemoSyncTick.class, new DemSyncTickHandler());

        // for match data
        H.put(CSVCMsg_PacketEntities.class, new SvcPacketEntitiesHandler());
        H.put(CSVCMsg_UpdateStringTable.class, new SvcUpdateStringTableHandler());

    }

    public static <T> void apply(T message, Match match) {
        @SuppressWarnings("unchecked")
        Handler<T> h = (Handler<T>) H.get(message.getClass());
        if (h != null) {
            h.apply(message, match);
        }
    }

}
