package clarity.parser.packethandler;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.javatuples.Pair;

import clarity.decoder.PacketEntitiesDecoder;
import clarity.decoder.StringTableDecoder;
import clarity.match.Match;
import clarity.model.Entity;
import clarity.model.EntityCollection;
import clarity.model.PVS;
import clarity.model.SendTable;
import clarity.model.StringTable;

import com.dota2.proto.Demo.CDemoClassInfo;
import com.dota2.proto.Demo.CDemoClassInfo.class_t;
import com.dota2.proto.Demo.CDemoConsoleCmd;
import com.dota2.proto.Demo.CDemoCustomData;
import com.dota2.proto.Demo.CDemoCustomDataCallbacks;
import com.dota2.proto.Demo.CDemoFileHeader;
import com.dota2.proto.Demo.CDemoFileInfo;
import com.dota2.proto.Demo.CDemoFullPacket;
import com.dota2.proto.Demo.CDemoPacket;
import com.dota2.proto.Demo.CDemoSendTables;
import com.dota2.proto.Demo.CDemoStop;
import com.dota2.proto.Demo.CDemoStringTables;
import com.dota2.proto.Demo.CDemoSyncTick;
import com.dota2.proto.Demo.CDemoUserCmd;
import com.dota2.proto.Demo.EDemoCommands;
import com.dota2.proto.Netmessages.CNETMsg_SetConVar;
import com.dota2.proto.Netmessages.CNETMsg_SignonState;
import com.dota2.proto.Netmessages.CNETMsg_Tick;
import com.dota2.proto.Netmessages.CSVCMsg_ClassInfo;
import com.dota2.proto.Netmessages.CSVCMsg_CreateStringTable;
import com.dota2.proto.Netmessages.CSVCMsg_GameEvent;
import com.dota2.proto.Netmessages.CSVCMsg_GameEventList;
import com.dota2.proto.Netmessages.CSVCMsg_Menu;
import com.dota2.proto.Netmessages.CSVCMsg_PacketEntities;
import com.dota2.proto.Netmessages.CSVCMsg_SendTable;
import com.dota2.proto.Netmessages.CSVCMsg_ServerInfo;
import com.dota2.proto.Netmessages.CSVCMsg_SetView;
import com.dota2.proto.Netmessages.CSVCMsg_Sounds;
import com.dota2.proto.Netmessages.CSVCMsg_TempEntities;
import com.dota2.proto.Netmessages.CSVCMsg_UpdateStringTable;
import com.dota2.proto.Netmessages.CSVCMsg_UserMessage;
import com.dota2.proto.Netmessages.CSVCMsg_VoiceData;
import com.dota2.proto.Netmessages.CSVCMsg_VoiceInit;
import com.dota2.proto.Netmessages.NET_Messages;
import com.dota2.proto.Netmessages.SVC_Messages;
import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;

public class PacketHandlerRegistry {

	public static final Map<Integer, PacketHandler<?>> DEMO = new TreeMap<Integer, PacketHandler<?>>();
	public static final Map<Integer, PacketHandler<?>> EMBED = new TreeMap<Integer, PacketHandler<?>>();
	
	static {
		DEMO.put(EDemoCommands.DEM_FileHeader_VALUE, new DemoPacketHandler<CDemoFileHeader>(CDemoFileHeader.class) {
			@Override
			public void apply(CDemoFileHeader message, Match match) {
				//System.out.println(message);
			}
		});
		DEMO.put(EDemoCommands.DEM_Stop_VALUE, new DemoPacketHandler<CDemoStop>(CDemoStop.class));
		DEMO.put(EDemoCommands.DEM_FileInfo_VALUE, new DemoPacketHandler<CDemoFileInfo>(CDemoFileInfo.class));
		DEMO.put(EDemoCommands.DEM_SendTables_VALUE, new EmbedPacketHandler<CDemoSendTables>(CDemoSendTables.class) { protected ByteString getData(GeneratedMessage message) { return ((CDemoSendTables) message).getData(); }});
		DEMO.put(EDemoCommands.DEM_SyncTick_VALUE, new DemoPacketHandler<CDemoSyncTick>(CDemoSyncTick.class));
		DEMO.put(EDemoCommands.DEM_ClassInfo_VALUE, new DemoPacketHandler<CDemoClassInfo>(CDemoClassInfo.class) {
			@Override
			public void apply(CDemoClassInfo message, Match match) {
				for (class_t ct : message.getClassesList()) {
					match.getClassByDT().put(ct.getTableName(), ct.getClassId());
				}
			}
		});
		DEMO.put(EDemoCommands.DEM_StringTables_VALUE, new DemoPacketHandler<CDemoStringTables>(CDemoStringTables.class) {
			@Override
			public void apply(CDemoStringTables message, Match match) {
				//System.out.println(message);
			}
		});
		DEMO.put(EDemoCommands.DEM_Packet_VALUE, new EmbedPacketHandler<CDemoPacket>(CDemoPacket.class) { protected ByteString getData(GeneratedMessage message) { return ((CDemoPacket) message).getData(); }});
		DEMO.put(EDemoCommands.DEM_SignonPacket_VALUE, new EmbedPacketHandler<CDemoPacket>(CDemoPacket.class) { protected ByteString getData(GeneratedMessage message) { return ((CDemoPacket) message).getData(); }});
		DEMO.put(EDemoCommands.DEM_ConsoleCmd_VALUE, new DemoPacketHandler<CDemoConsoleCmd>(CDemoConsoleCmd.class));
		DEMO.put(EDemoCommands.DEM_CustomData_VALUE, new DemoPacketHandler<CDemoCustomData>(CDemoCustomData.class));
		DEMO.put(EDemoCommands.DEM_CustomDataCallbacks_VALUE, new DemoPacketHandler<CDemoCustomDataCallbacks>(CDemoCustomDataCallbacks.class));
		DEMO.put(EDemoCommands.DEM_UserCmd_VALUE, new DemoPacketHandler<CDemoUserCmd>(CDemoUserCmd.class) {
			@Override
			public void apply(CDemoUserCmd message, Match match) {
				System.out.println(message);
			}
		});
		DEMO.put(EDemoCommands.DEM_FullPacket_VALUE, new DemoPacketHandler<CDemoFullPacket>(CDemoFullPacket.class));
		
		
		
		
		EMBED.put(NET_Messages.net_SetConVar_VALUE, new DemoPacketHandler<CNETMsg_SetConVar>(CNETMsg_SetConVar.class));
		EMBED.put(NET_Messages.net_SignonState_VALUE, new DemoPacketHandler<CNETMsg_SignonState>(CNETMsg_SignonState.class));
		EMBED.put(NET_Messages.net_Tick_VALUE, new DemoPacketHandler<CNETMsg_Tick>(CNETMsg_Tick.class) {
			@Override
			public void apply(CNETMsg_Tick message, Match match) {
				//System.out.println(message);
			}
		});
		EMBED.put(SVC_Messages.svc_ClassInfo_VALUE, new DemoPacketHandler<CSVCMsg_ClassInfo>(CSVCMsg_ClassInfo.class) {
			@Override
			public void apply(CSVCMsg_ClassInfo message, Match match) {
				//System.out.println(message);
			}
		});
		EMBED.put(SVC_Messages.svc_CreateStringTable_VALUE, new DemoPacketHandler<CSVCMsg_CreateStringTable>(CSVCMsg_CreateStringTable.class) {
			@Override
			public void apply(CSVCMsg_CreateStringTable message, Match match) {
				StringTable table = new StringTable(message);
				StringTableDecoder.decode(table, message.getStringData().toByteArray(), message.getNumEntries());
				match.getStringTables().add(table);
			}
		});
		EMBED.put(SVC_Messages.svc_GameEventList_VALUE, new DemoPacketHandler<CSVCMsg_GameEventList>(CSVCMsg_GameEventList.class) {
			@Override
			public void apply(CSVCMsg_GameEventList message, Match match) {
				//System.out.println(message);
			}
		});
		EMBED.put(SVC_Messages.svc_Menu_VALUE, new DemoPacketHandler<CSVCMsg_Menu>(CSVCMsg_Menu.class) {
			@Override
			public void apply(CSVCMsg_Menu message, Match match) {
				//System.out.println(message);
			}
		});
		EMBED.put(SVC_Messages.svc_PacketEntities_VALUE, new DemoPacketHandler<CSVCMsg_PacketEntities>(CSVCMsg_PacketEntities.class) {
			@Override
			public void apply(CSVCMsg_PacketEntities message, Match match) {
				EntityCollection entities = match.getEntityCollection();
				List<Pair<PVS, Entity>> changes = new PacketEntitiesDecoder(
					message.getEntityData().toByteArray(), 
					message.getUpdatedEntries(),
					message.getIsDelta(),
					match.getReceivePropsByClass(),
					match.getStringTableByName("instancebaseline")
				).decode(match.getEntityCollection());
				for (Pair<PVS, Entity> change : changes) {
					PVS pvs = change.getValue0();
					Entity entity = change.getValue1();
					switch(pvs) {
						case ENTER:
							entities.put(entity.getIndex(), entity);
							break;
							
						case PRESERVE:
							entities.get(entity.getIndex()).updateFrom(entity);
							break;
							
						case LEAVE:
							break;
							
						case LEAVE_AND_DELETE:
							entities.put(entity.getIndex(), null);
							break;
					}
				}
			}
		});
		EMBED.put(SVC_Messages.svc_SendTable_VALUE, new DemoPacketHandler<CSVCMsg_SendTable>(CSVCMsg_SendTable.class) {
			@Override
			public void apply(CSVCMsg_SendTable message, Match match) {
				SendTable st = new SendTable(message);
				match.getSendTableByDT().put(st.getMessage().getNetTableName(), st);
			}
		});
		EMBED.put(SVC_Messages.svc_ServerInfo_VALUE, new DemoPacketHandler<CSVCMsg_ServerInfo>(CSVCMsg_ServerInfo.class));
		EMBED.put(SVC_Messages.svc_SetView_VALUE, new DemoPacketHandler<CSVCMsg_SetView>(CSVCMsg_SetView.class) {
			@Override
			public void apply(CSVCMsg_SetView message, Match match) {
				//System.out.println(message);
			}
		});
		EMBED.put(SVC_Messages.svc_Sounds_VALUE, new DemoPacketHandler<CSVCMsg_Sounds>(CSVCMsg_Sounds.class));
		EMBED.put(SVC_Messages.svc_TempEntities_VALUE, new DemoPacketHandler<CSVCMsg_TempEntities>(CSVCMsg_TempEntities.class) {
			@Override
			public void apply(CSVCMsg_TempEntities message, Match match) {
//				List<Pair<PVS, Entity>> changes = new PacketEntitiesDecoder(
//					message.getEntityData().toByteArray(), 
//					message.getNumEntries(),
//					false,
//					match.getReceivePropsByClass()
//				).decode(match.getEntityCollection());
//				System.out.println(changes);
			}
		});
		EMBED.put(SVC_Messages.svc_UpdateStringTable_VALUE, new DemoPacketHandler<CSVCMsg_UpdateStringTable>(CSVCMsg_UpdateStringTable.class) {
			@Override
			public void apply(CSVCMsg_UpdateStringTable message, Match match) {
				StringTable table = match.getStringTables().get(message.getTableId());
				//System.out.println("updating " + table.getName());
				StringTableDecoder.decode(table, message.getStringData().toByteArray(), message.getNumChangedEntries());
			}
		});
		EMBED.put(SVC_Messages.svc_VoiceInit_VALUE, new DemoPacketHandler<CSVCMsg_VoiceInit>(CSVCMsg_VoiceInit.class));
		EMBED.put(SVC_Messages.svc_VoiceData_VALUE, new DemoPacketHandler<CSVCMsg_VoiceData>(CSVCMsg_VoiceData.class));
		EMBED.put(SVC_Messages.svc_GameEvent_VALUE, new DemoPacketHandler<CSVCMsg_GameEvent>(CSVCMsg_GameEvent.class) {
			@Override
			public void apply(CSVCMsg_GameEvent message, Match match) {
				//System.out.println(message);
			}
		});
		EMBED.put(SVC_Messages.svc_UserMessage_VALUE, new DemoPacketHandler<CSVCMsg_UserMessage>(CSVCMsg_UserMessage.class) {
			@Override
			public void apply(CSVCMsg_UserMessage message, Match match) {
				//System.out.println(message);
			}
		});
	}
	
	public static PacketHandler<?> forDemo(int kind) {
		return DEMO.get(kind);
	}
	
	public static PacketHandler<?> forEmbed(int kind) {
		return EMBED.get(kind);
	}
		
}
