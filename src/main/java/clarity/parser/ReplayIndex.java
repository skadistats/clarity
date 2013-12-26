package clarity.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.xerial.snappy.Snappy;

import clarity.iterator.BidiIterator;
import clarity.iterator.ReplayIndexIterator;

import com.dota2.proto.Demo.CDemoClassInfo;
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
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;

public class ReplayIndex {

    private final List<Peek> index = new ArrayList<Peek>();

    public ReplayIndex(CodedInputStream s) throws IOException {
        int tick = 0;
        int kind = 0;
        do {
            kind = s.readRawVarint32();
            boolean isCompressed = (kind & EDemoCommands.DEM_IsCompressed_VALUE) != 0;
            kind &= ~EDemoCommands.DEM_IsCompressed_VALUE;
            tick = s.readRawVarint32();
            int size = s.readRawVarint32();
            byte[] data = s.readRawBytes(size);
            if (isCompressed) {
                data = Snappy.uncompress(data);
            }
            GeneratedMessage message = parseTopLevel(kind, data);
            if (message == null) {
                System.out.println("unknown top level message of kind " + kind);
                continue;
            }
            ByteString embeddedData = getEmbeddedData(message);
            if (embeddedData != null) {
                CodedInputStream ss = CodedInputStream.newInstance(embeddedData.toByteArray());
                while (!ss.isAtEnd()) {
                    int subKind = ss.readRawVarint32();
                    int subSize = ss.readRawVarint32();
                    byte[] subData = ss.readRawBytes(subSize);
                    GeneratedMessage subMessage = parseEmbedded(subKind, subData);
                    if (subMessage == null) {
                        System.out.println("unknown embedded message of kind " + subKind);
                        continue;
                    }
                    index.add(new Peek(tick, subMessage));
                }

            } else {
                index.add(new Peek(tick, message));
            }
        } while (kind != 0);
    }

    public Peek get(int i) {
        return index.get(i);
    }

    public int size() {
        return index.size();
    }

    public int nextIndexOf(Class<? extends GeneratedMessage> clazz, int pos) {
        for (int i = pos; i < index.size(); i++) {
            if (clazz.isAssignableFrom(index.get(i).getMessage().getClass())) {
                return i;
            }
        }
        return -1;
    }

    public BidiIterator<Peek> prologueIterator() {
        int syncIdx = nextIndexOf(CDemoSyncTick.class, 0);
        return new ReplayIndexIterator(this, 0, syncIdx);
    }

    public BidiIterator<Peek> matchIterator() {
        int syncIdx = nextIndexOf(CDemoSyncTick.class, 0);
        return new ReplayIndexIterator(this, syncIdx + 1, index.size() - 1);
    }

    private ByteString getEmbeddedData(GeneratedMessage message) throws InvalidProtocolBufferException {
        if (message instanceof CDemoPacket) {
            return ((CDemoPacket) message).getData();
        } else if (message instanceof CDemoSendTables) {
            return ((CDemoSendTables) message).getData();
        }
        return null;
    }

    private GeneratedMessage parseTopLevel(int kind, byte[] data) throws InvalidProtocolBufferException {
        switch (EDemoCommands.valueOf(kind)) {
        case DEM_ClassInfo:
            return CDemoClassInfo.parseFrom(data);
        case DEM_ConsoleCmd:
            return CDemoConsoleCmd.parseFrom(data);
        case DEM_CustomData:
            return CDemoCustomData.parseFrom(data);
        case DEM_CustomDataCallbacks:
            return CDemoCustomDataCallbacks.parseFrom(data);
        case DEM_FileHeader:
            return CDemoFileHeader.parseFrom(data);
        case DEM_FileInfo:
            return CDemoFileInfo.parseFrom(data);
        case DEM_FullPacket:
            return CDemoFullPacket.parseFrom(data);
        case DEM_Packet:
            return CDemoPacket.parseFrom(data);
        case DEM_SendTables:
            return CDemoSendTables.parseFrom(data);
        case DEM_SignonPacket:
            return CDemoPacket.parseFrom(data);
        case DEM_StringTables:
            return CDemoStringTables.parseFrom(data);
        case DEM_Stop:
            return CDemoStop.parseFrom(data);
        case DEM_SyncTick:
            return CDemoSyncTick.parseFrom(data);
        case DEM_UserCmd:
            return CDemoUserCmd.parseFrom(data);
        default:
            return null;
        }
    }

    private GeneratedMessage parseEmbedded(int kind, byte[] data) throws InvalidProtocolBufferException {
        switch (kind) {
        case NET_Messages.net_SetConVar_VALUE:
            return CNETMsg_SetConVar.parseFrom(data);
        case NET_Messages.net_SignonState_VALUE:
            return CNETMsg_SignonState.parseFrom(data);
        case NET_Messages.net_Tick_VALUE:
            return CNETMsg_Tick.parseFrom(data);

        case SVC_Messages.svc_ClassInfo_VALUE:
            return CSVCMsg_ClassInfo.parseFrom(data);
        case SVC_Messages.svc_CreateStringTable_VALUE:
            return CSVCMsg_CreateStringTable.parseFrom(data);
        case SVC_Messages.svc_GameEvent_VALUE:
            return CSVCMsg_GameEvent.parseFrom(data);
        case SVC_Messages.svc_GameEventList_VALUE:
            return CSVCMsg_GameEventList.parseFrom(data);
        case SVC_Messages.svc_Menu_VALUE:
            return CSVCMsg_Menu.parseFrom(data);
        case SVC_Messages.svc_PacketEntities_VALUE:
            return CSVCMsg_PacketEntities.parseFrom(data);
        case SVC_Messages.svc_SendTable_VALUE:
            return CSVCMsg_SendTable.parseFrom(data);
        case SVC_Messages.svc_ServerInfo_VALUE:
            return CSVCMsg_ServerInfo.parseFrom(data);
        case SVC_Messages.svc_SetView_VALUE:
            return CSVCMsg_SetView.parseFrom(data);
        case SVC_Messages.svc_Sounds_VALUE:
            return CSVCMsg_Sounds.parseFrom(data);
        case SVC_Messages.svc_TempEntities_VALUE:
            return CSVCMsg_TempEntities.parseFrom(data);
        case SVC_Messages.svc_UpdateStringTable_VALUE:
            return CSVCMsg_UpdateStringTable.parseFrom(data);
        case SVC_Messages.svc_UserMessage_VALUE:
            return CSVCMsg_UserMessage.parseFrom(data);
        case SVC_Messages.svc_VoiceInit_VALUE:
            return CSVCMsg_VoiceInit.parseFrom(data);
        case SVC_Messages.svc_VoiceData_VALUE:
            return CSVCMsg_VoiceData.parseFrom(data);

        default:
            return null;
        }
    }

}
