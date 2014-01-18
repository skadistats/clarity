package clarity.parser;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.dota2.proto.Demo;
import com.dota2.proto.Netmessages;
import com.dota2.proto.Networkbasetypes;
import com.google.protobuf.GeneratedMessage;

public class PacketTypes {

    public static final Map<Integer, Class<? extends GeneratedMessage>> DEMO;
    static {
        DEMO = new HashMap<Integer, Class<? extends GeneratedMessage>>();
        DEMO.put(Demo.EDemoCommands.DEM_ClassInfo_VALUE, Demo.CDemoClassInfo.class);
        DEMO.put(Demo.EDemoCommands.DEM_ConsoleCmd_VALUE, Demo.CDemoConsoleCmd.class);
        DEMO.put(Demo.EDemoCommands.DEM_CustomData_VALUE, Demo.CDemoCustomData.class);
        DEMO.put(Demo.EDemoCommands.DEM_CustomDataCallbacks_VALUE, Demo.CDemoCustomDataCallbacks.class);
        DEMO.put(Demo.EDemoCommands.DEM_FileHeader_VALUE, Demo.CDemoFileHeader.class);
        DEMO.put(Demo.EDemoCommands.DEM_FileInfo_VALUE, Demo.CDemoFileInfo.class);
        DEMO.put(Demo.EDemoCommands.DEM_FullPacket_VALUE, Demo.CDemoFullPacket.class);
        DEMO.put(Demo.EDemoCommands.DEM_Packet_VALUE, Demo.CDemoPacket.class);
        DEMO.put(Demo.EDemoCommands.DEM_SendTables_VALUE, Demo.CDemoSendTables.class);
        DEMO.put(Demo.EDemoCommands.DEM_SignonPacket_VALUE, Demo.CDemoPacket.class);
        DEMO.put(Demo.EDemoCommands.DEM_StringTables_VALUE, Demo.CDemoStringTables.class);
        DEMO.put(Demo.EDemoCommands.DEM_Stop_VALUE, Demo.CDemoStop.class);
        DEMO.put(Demo.EDemoCommands.DEM_SyncTick_VALUE, Demo.CDemoSyncTick.class);
        DEMO.put(Demo.EDemoCommands.DEM_UserCmd_VALUE, Demo.CDemoUserCmd.class);
    }
    
    public static final Map<Integer, Class<? extends GeneratedMessage>> EMBED;
    static {
        EMBED = new HashMap<Integer, Class<? extends GeneratedMessage>>();
        EMBED.put(Netmessages.NET_Messages.net_SetConVar_VALUE, Netmessages.CNETMsg_SetConVar.class);
        EMBED.put(Netmessages.NET_Messages.net_SignonState_VALUE, Netmessages.CNETMsg_SignonState.class);
        EMBED.put(Netmessages.NET_Messages.net_Tick_VALUE, Netmessages.CNETMsg_Tick.class);
        EMBED.put(Netmessages.SVC_Messages.svc_ClassInfo_VALUE, Netmessages.CSVCMsg_ClassInfo.class);
        EMBED.put(Netmessages.SVC_Messages.svc_CreateStringTable_VALUE, Netmessages.CSVCMsg_CreateStringTable.class);
        EMBED.put(Netmessages.SVC_Messages.svc_GameEvent_VALUE, Networkbasetypes.CSVCMsg_GameEvent.class);
        EMBED.put(Netmessages.SVC_Messages.svc_GameEventList_VALUE, Netmessages.CSVCMsg_GameEventList.class);
        EMBED.put(Netmessages.SVC_Messages.svc_Menu_VALUE, Netmessages.CSVCMsg_Menu.class);
        EMBED.put(Netmessages.SVC_Messages.svc_PacketEntities_VALUE, Netmessages.CSVCMsg_PacketEntities.class);
        EMBED.put(Netmessages.SVC_Messages.svc_SendTable_VALUE, Netmessages.CSVCMsg_SendTable.class);
        EMBED.put(Netmessages.SVC_Messages.svc_ServerInfo_VALUE, Netmessages.CSVCMsg_ServerInfo.class);
        EMBED.put(Netmessages.SVC_Messages.svc_SetView_VALUE, Netmessages.CSVCMsg_SetView.class);
        EMBED.put(Netmessages.SVC_Messages.svc_Sounds_VALUE, Netmessages.CSVCMsg_Sounds.class);
        EMBED.put(Netmessages.SVC_Messages.svc_TempEntities_VALUE, Netmessages.CSVCMsg_TempEntities.class);
        EMBED.put(Netmessages.SVC_Messages.svc_UpdateStringTable_VALUE, Netmessages.CSVCMsg_UpdateStringTable.class);
        EMBED.put(Netmessages.SVC_Messages.svc_UserMessage_VALUE, Networkbasetypes.CSVCMsg_UserMessage.class);
        EMBED.put(Netmessages.SVC_Messages.svc_VoiceInit_VALUE, Netmessages.CSVCMsg_VoiceInit.class);
        EMBED.put(Netmessages.SVC_Messages.svc_VoiceData_VALUE, Netmessages.CSVCMsg_VoiceData.class);
    }
    
    private static final Map<Class<? extends GeneratedMessage>, Method> PARSE_METHODS = new HashMap<Class<? extends GeneratedMessage>, Method>() {
        private static final long serialVersionUID = -6842762498712492043L;
        @SuppressWarnings("unchecked")
        @Override
        public Method get(Object key) {
            Method m = super.get(key);
            if (m == null) {
                try {
                    Class<? extends GeneratedMessage> clazz = (Class<? extends GeneratedMessage>) key;
                    m = clazz.getMethod("parseFrom", byte[].class);
                    put(clazz, m);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
            return m;
        }
    };
    
    @SuppressWarnings("unchecked")
    public static <T extends GeneratedMessage> T parse(Class<T> clazz, byte[] data) {
        try {
            return (T) PARSE_METHODS.get(clazz).invoke(null, data);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        } catch (IllegalArgumentException e) {
            throw new RuntimeException(e);
        } catch (InvocationTargetException e) {
            throw new RuntimeException(e);
        }
    }
    
}
