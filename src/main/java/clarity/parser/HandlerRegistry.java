package clarity.parser;

import java.util.HashMap;
import java.util.Map;

import clarity.match.Match;
import clarity.parser.handler.DemClassInfoHandler;
import clarity.parser.handler.DemStringTablesHandler;
import clarity.parser.handler.DemSyncTickHandler;
import clarity.parser.handler.NetTickHandler;
import clarity.parser.handler.SvcCreateStringTableHandler;
import clarity.parser.handler.SvcGameEventHandler;
import clarity.parser.handler.SvcGameEventListHandler;
import clarity.parser.handler.SvcPacketEntitiesHandler;
import clarity.parser.handler.SvcSendTableHandler;
import clarity.parser.handler.SvcUpdateStringTableHandler;
import clarity.parser.handler.UserMsgCreateLinearProjectileHandler;
import clarity.parser.handler.UserMsgDestroyLinearProjectileHandler;
import clarity.parser.handler.UserMsgDodgeTrackingProjectilesHandler;
import clarity.parser.handler.UserMsgGamerulesStateChangedHandler;

import com.dota2.proto.Demo.CDemoClassInfo;
import com.dota2.proto.Demo.CDemoStringTables;
import com.dota2.proto.Demo.CDemoSyncTick;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_CreateLinearProjectile;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_DestroyLinearProjectile;
import com.dota2.proto.DotaUsermessages.CDOTAUserMsg_DodgeTrackingProjectiles;
import com.dota2.proto.DotaUsermessages.CDOTA_UM_GamerulesStateChanged;
import com.dota2.proto.Netmessages.CNETMsg_Tick;
import com.dota2.proto.Netmessages.CSVCMsg_CreateStringTable;
import com.dota2.proto.Netmessages.CSVCMsg_GameEventList;
import com.dota2.proto.Netmessages.CSVCMsg_PacketEntities;
import com.dota2.proto.Netmessages.CSVCMsg_SendTable;
import com.dota2.proto.Netmessages.CSVCMsg_UpdateStringTable;
import com.dota2.proto.Networkbasetypes.CSVCMsg_GameEvent;

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
        H.put(CSVCMsg_GameEventList.class, new SvcGameEventListHandler());
        
        // for match data
        H.put(CNETMsg_Tick.class, new NetTickHandler());
        H.put(CSVCMsg_PacketEntities.class, new SvcPacketEntitiesHandler());
        H.put(CSVCMsg_UpdateStringTable.class, new SvcUpdateStringTableHandler());
        H.put(CSVCMsg_GameEvent.class, new SvcGameEventHandler());
        H.put(CDOTAUserMsg_CreateLinearProjectile.class, new UserMsgCreateLinearProjectileHandler());
        H.put(CDOTAUserMsg_DestroyLinearProjectile.class, new UserMsgDestroyLinearProjectileHandler());
        H.put(CDOTAUserMsg_DodgeTrackingProjectiles.class, new UserMsgDodgeTrackingProjectilesHandler());
        H.put(CDOTA_UM_GamerulesStateChanged.class, new UserMsgGamerulesStateChangedHandler());

    }

    public static <T> void apply(T message, Match match) {
        @SuppressWarnings("unchecked")
        Handler<T> h = (Handler<T>) H.get(message.getClass());
        if (h != null) {
            h.apply(message, match);
        }
    }

}
