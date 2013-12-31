package clarity.parser;

import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import clarity.model.UserMessageType;

import com.dota2.proto.Demo.CDemoClassInfo;
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
import com.dota2.proto.Netmessages.CNETMsg_Tick;
import com.dota2.proto.Netmessages.CSVCMsg_CreateStringTable;
import com.dota2.proto.Netmessages.CSVCMsg_GameEventList;
import com.dota2.proto.Netmessages.CSVCMsg_PacketEntities;
import com.dota2.proto.Netmessages.CSVCMsg_SendTable;
import com.dota2.proto.Netmessages.CSVCMsg_ServerInfo;
import com.dota2.proto.Netmessages.CSVCMsg_UpdateStringTable;
import com.dota2.proto.Netmessages.NET_Messages;
import com.dota2.proto.Netmessages.SVC_Messages;
import com.dota2.proto.Networkbasetypes.CSVCMsg_GameEvent;
import com.dota2.proto.Networkbasetypes.CSVCMsg_UserMessage;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;

public class DemoInputStream {

    private enum State {
        TOP, EMBED
    };

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final CodedInputStream s; // main stream
    private CodedInputStream ss = null; // stream for embedded packet
    private int n = -1;
    private int tick = 0;
    private int peekTick = 0;
    private boolean full = false;
    private State state = State.TOP;

    public DemoInputStream(InputStream s) {
        this(CodedInputStream.newInstance(s));
    }

    public DemoInputStream(CodedInputStream s) {
        this.s = s;
    }

    public Peek read() throws IOException {
        while (!s.isAtEnd()) {
            switch (state) {
                case TOP:
                    int kind = s.readRawVarint32();
                    boolean isCompressed = (kind & EDemoCommands.DEM_IsCompressed_VALUE) != 0;
                    kind &= ~EDemoCommands.DEM_IsCompressed_VALUE;
                    peekTick = s.readRawVarint32();
                    int size = s.readRawVarint32();
                    byte[] data = s.readRawBytes(size);
                    if (isCompressed) {
                        data = Snappy.uncompress(data);
                    }
                    GeneratedMessage message = parseTopLevel(kind, data);
                    if (message == null) {
                        log.warn("unknown top level message of kind {}", kind);
                        continue;
                    }
                    full = false;
                    if (message instanceof CDemoPacket) {
                        ss = CodedInputStream.newInstance(((CDemoPacket) message).getData().toByteArray());
                        state = State.EMBED;
                        continue;
                    } else if (message instanceof CDemoSendTables) {
                        ss = CodedInputStream.newInstance(((CDemoSendTables) message).getData().toByteArray());
                        state = State.EMBED;
                        continue;
                    } else if (message instanceof CDemoFullPacket) {
                        CDemoFullPacket fullMessage = (CDemoFullPacket)message;
                        ss = CodedInputStream.newInstance(fullMessage.getPacket().getData().toByteArray());
                        state = State.EMBED;
                        full = true;
                        return new Peek(++n, tick, peekTick, true, fullMessage.getStringTable());
                    } else {
                        return new Peek(++n, tick, peekTick, false, message);
                    }
                    

                case EMBED:
                    if (ss.isAtEnd()) {
                        ss = null;
                        state = State.TOP;
                        continue;
                    }
                    int subKind = ss.readRawVarint32();
                    int subSize = ss.readRawVarint32();
                    byte[] subData = ss.readRawBytes(subSize);
                    GeneratedMessage subMessage = parseEmbedded(subKind, subData);
                    if (subMessage == null) {
                        //log.warn("unknown embedded message of kind {}", subKind);
                        continue;
                    }
                    if (subMessage instanceof CSVCMsg_UserMessage) {
                        CSVCMsg_UserMessage userMessage = (CSVCMsg_UserMessage) subMessage;
                        UserMessageType umt = UserMessageType.forId(userMessage.getMsgType());
                        if (umt == null) {
                            log.warn("unknown usermessage of kind {}", userMessage.getMsgType());
                            continue;
                        } else if (umt.getClazz() == null) {
                            log.warn("no protobuf class for usermessage of type {}", umt);
                            continue;
                        } else { 
                            GeneratedMessage decodedUserMessage = umt.parseFrom(userMessage.getMsgData());
                            return new Peek(++n, tick, peekTick, full, decodedUserMessage);
                        }
                    } else if (subMessage instanceof CNETMsg_Tick) {
                        tick = ((CNETMsg_Tick) subMessage).getTick();
                    } else {
                        return new Peek(++n, tick, peekTick, full, subMessage);
                    }
                    continue;
            }
        }
        return null;
    }

    private GeneratedMessage parseTopLevel(int kind, byte[] data) throws InvalidProtocolBufferException {
        switch (EDemoCommands.valueOf(kind)) {
        case DEM_ClassInfo:
            return CDemoClassInfo.parseFrom(data);
            // case DEM_ConsoleCmd:
            // return CDemoConsoleCmd.parseFrom(data);
            // case DEM_CustomData:
            // return CDemoCustomData.parseFrom(data);
            // case DEM_CustomDataCallbacks:
            // return CDemoCustomDataCallbacks.parseFrom(data);
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
//        case NET_Messages.net_SetConVar_VALUE:
//            return CNETMsg_SetConVar.parseFrom(data);
//        case NET_Messages.net_SignonState_VALUE:
//            return CNETMsg_SignonState.parseFrom(data);
        case NET_Messages.net_Tick_VALUE:
            return CNETMsg_Tick.parseFrom(data);

//        case SVC_Messages.svc_ClassInfo_VALUE:
//            return CSVCMsg_ClassInfo.parseFrom(data);
        case SVC_Messages.svc_CreateStringTable_VALUE:
            return CSVCMsg_CreateStringTable.parseFrom(data);
        case SVC_Messages.svc_GameEvent_VALUE:
            return CSVCMsg_GameEvent.parseFrom(data);
        case SVC_Messages.svc_GameEventList_VALUE:
            return CSVCMsg_GameEventList.parseFrom(data);
//        case SVC_Messages.svc_Menu_VALUE:
//            return CSVCMsg_Menu.parseFrom(data);
        case SVC_Messages.svc_PacketEntities_VALUE:
            return CSVCMsg_PacketEntities.parseFrom(data);
        case SVC_Messages.svc_SendTable_VALUE:
            return CSVCMsg_SendTable.parseFrom(data);
        case SVC_Messages.svc_ServerInfo_VALUE:
            return CSVCMsg_ServerInfo.parseFrom(data);
//        case SVC_Messages.svc_SetView_VALUE:
//            return CSVCMsg_SetView.parseFrom(data);
//        case SVC_Messages.svc_Sounds_VALUE:
//            return CSVCMsg_Sounds.parseFrom(data);
//        case SVC_Messages.svc_TempEntities_VALUE:
//            return CSVCMsg_TempEntities.parseFrom(data);
        case SVC_Messages.svc_UpdateStringTable_VALUE:
            return CSVCMsg_UpdateStringTable.parseFrom(data);
        case SVC_Messages.svc_UserMessage_VALUE:
            return CSVCMsg_UserMessage.parseFrom(data);
//        case SVC_Messages.svc_VoiceInit_VALUE:
//            return CSVCMsg_VoiceInit.parseFrom(data);
//        case SVC_Messages.svc_VoiceData_VALUE:
//            return CSVCMsg_VoiceData.parseFrom(data);

        default:
            return null;
        }
    }
    

}
