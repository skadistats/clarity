package clarity.parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import clarity.model.PacketType;
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
import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.InvalidProtocolBufferException;

public class ReplayIndex {

    private final Logger log = LoggerFactory.getLogger(getClass());
    
    private final List<Peek> index = new ArrayList<Peek>();
    private int skew = 0; // the skew between peek tick and net tick
    private int tick = 0; // the number of ticks in this replay
    private int syncIdx = 0; // the index of the sync packet
    
    public ReplayIndex(CodedInputStream s) throws IOException {
        boolean sync = false;
        int peekTick = 0;
        int kind = 0;
        do {
            kind = s.readRawVarint32();
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
            if (message instanceof CDemoPacket) {
                processEmbeddedMessage(((CDemoPacket) message).getData(), false, peekTick, sync);
            } else if (message instanceof CDemoSendTables) {
                processEmbeddedMessage(((CDemoSendTables) message).getData(), false, peekTick, sync);
            } else if (message instanceof CDemoFullPacket) {
                CDemoFullPacket fullMessage = (CDemoFullPacket)message;
                index.add(new Peek(index.size(), tick, peekTick, true, ((CDemoFullPacket) message).getStringTable()));
                processEmbeddedMessage(fullMessage.getPacket().getData(), true, peekTick, sync);
            } else {
                index.add(new Peek(index.size(), tick, peekTick, false, message));
            }
            sync = message instanceof CDemoSyncTick;
            if (sync) {
               syncIdx = index.size() - 1; 
            }
        } while (kind != 0);
    }
    
    private void processEmbeddedMessage(ByteString embeddedData, boolean full, int peekTick, boolean sync) throws IOException {
        CodedInputStream ss = CodedInputStream.newInstance(embeddedData.toByteArray());
        while (!ss.isAtEnd()) {
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
                }
                if (umt.getClazz() == null) {
                    log.warn("no protobuf class for usermessage of type {}", umt);
                }
                GeneratedMessage decodedUserMessage = umt.parseFrom(userMessage.getMsgData());
                index.add(new Peek(index.size(), tick, peekTick, full, decodedUserMessage));
            } else {
                if (subMessage instanceof CNETMsg_Tick) {
                    int netTick = ((CNETMsg_Tick) subMessage).getTick(); 
                    if (sync) {
                        skew = ((CNETMsg_Tick) subMessage).getTick();
                    }
                    tick = netTick - skew;
                } else {
                    index.add(new Peek(index.size(), tick, peekTick, full, subMessage));
                }
            }
        }
    }

    public Peek get(int i) {
        return index.get(i);
    }

    public int size() {
        return index.size();
    }
    
    public int getLastTick() {
        return tick;
    }
    
    private int indexForTick(List<Peek> list, int tick) {
        int a = -1; // lower bound 
        int b = list.size(); // upper bound
        while (a + 1 != b) {
            int  m = (a + b) >>> 1;
            if (list.get(m).getTick() < tick) {
                a = m;
            } else {
                b = m;
            }
        }
        return b;
    }

    public int nextIndexOf(Class<? extends GeneratedMessage> clazz, int pos) {
        for (int i = pos; i < index.size(); i++) {
            if (clazz.isAssignableFrom(index.get(i).getMessage().getClass())) {
                return i;
            }
        }
        return -1;
    }
    
    private List<Peek> prologueList() {
        return index.subList(0, syncIdx + 1);
    }

    private List<Peek> matchList() {
        return index.subList(syncIdx + 1, index.size());
    }
    
    public Iterator<Peek> prologueIterator() {
        return prologueList().iterator();
    }

    public Iterator<Peek> matchIterator() {
        return matchList().iterator();
    }
    
    public Iterator<Peek> matchIteratorForTicks(final int startTick, final int endTick, final PacketType packetType) {
        List<Peek> match = matchList();
        return 
            Iterators.filter(
                match.subList(indexForTick(match, startTick), indexForTick(match, endTick + 1)).iterator(), 
                packetType.getPredicate()
            );
    }
    
    public Iterator<Peek> filteringIteratorForTicks(final int startTick, final int endTick, final PacketType packetType, final Class<? extends GeneratedMessage> clazz) {
        return Iterators.filter(
            matchIteratorForTicks(startTick, endTick, packetType),
            new Predicate<Peek>() {
                @Override
                public boolean apply(Peek p) {
                    return clazz.isAssignableFrom(p.getMessage().getClass());
                }
            }
        );
    }
    
    public Iterator<Peek> skipToIterator(final int tick) {
        final Peek p = Iterators.getLast(filteringIteratorForTicks(0, tick, PacketType.FULL, CDemoStringTables.class)); 
        return Iterators.concat(
            filteringIteratorForTicks(0, p.getTick() - 1, PacketType.FULL, CDemoStringTables.class),
            matchIteratorForTicks(p.getTick(), p.getTick(), PacketType.FULL),
            matchIteratorForTicks(p.getTick() + 1, tick, PacketType.DELTA)
        );
    }

    private GeneratedMessage parseTopLevel(int kind, byte[] data) throws InvalidProtocolBufferException {
        switch (EDemoCommands.valueOf(kind)) {
        case DEM_ClassInfo:
            return CDemoClassInfo.parseFrom(data);
//        case DEM_ConsoleCmd:
//            return CDemoConsoleCmd.parseFrom(data);
//        case DEM_CustomData:
//            return CDemoCustomData.parseFrom(data);
//        case DEM_CustomDataCallbacks:
//            return CDemoCustomDataCallbacks.parseFrom(data);
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
