package skadistats.clarity.parser;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;

import skadistats.clarity.parser.Peek.BorderType;

import com.dota2.proto.Demo.CDemoFullPacket;
import com.dota2.proto.Demo.CDemoPacket;
import com.dota2.proto.Demo.CDemoSendTables;
import com.dota2.proto.Demo.CDemoStringTables;
import com.dota2.proto.Demo.EDemoCommands;
import com.dota2.proto.Netmessages.CNETMsg_Tick;
import com.dota2.proto.Networkbasetypes.CSVCMsg_UserMessage;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.GeneratedMessage;

public class DemoInputStream implements Closeable {

    private enum State {
        TOP, EMBED
    };

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Profile[] profile;
    private final CodedInputStream s; // main stream
    private final InputStream is; // stream being wrapped
    private CodedInputStream ss = null; // stream for embedded packet
    private int n = -1;
    private int tick = 0;
    private int peekTick = 0;
    private boolean full = false;
    private BorderType border = BorderType.NONE;
    private State state = State.TOP;

    public DemoInputStream(CodedInputStream s, InputStream is, Profile... profile) {
        this.s = s;
        this.is = is;
        this.profile = profile;
    }
    
    private boolean isFiltered(Class<?> clazz) {
        if (profile == null) {
            return false;
        }
        for (Profile p : profile) {
            if (p.contains(clazz)) {
                return false;
            }
        }
        return true;
    }
    
    private Peek genPeek(GeneratedMessage message) {
        Peek result = new Peek(++n, tick, peekTick, full, border, message);
        border = BorderType.NONE;
        return result;
    }

    public Peek read() throws IOException {
        while (!s.isAtEnd()) {
            switch (state) {
                case TOP:
                    int kind = s.readRawVarint32();
                    boolean isCompressed = (kind & EDemoCommands.DEM_IsCompressed_VALUE) == EDemoCommands.DEM_IsCompressed_VALUE;
                    kind &= ~EDemoCommands.DEM_IsCompressed_VALUE;
                    int nextPeekTick = s.readRawVarint32();
                    if (nextPeekTick != peekTick) {
                        border = border.addPeekTickBorder();
                    }
                    peekTick = nextPeekTick;
                    int size = s.readRawVarint32();
                    byte[] data = s.readRawBytes(size);
                    if (isCompressed) {
                        data = Snappy.uncompress(data);
                    }
                    Class<? extends GeneratedMessage> topClazz = PacketTypes.DEMO.get(kind);
                    if (topClazz == null) {
                        log.warn("unknown top level message of kind {}", kind);
                        continue;
                    }
                    GeneratedMessage message = PacketTypes.parse(topClazz, data);
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
                        if (!isFiltered(CDemoStringTables.class)) {
                            return genPeek(fullMessage.getStringTable());
                        }
                    } else if (!isFiltered(message.getClass())) {
                        return genPeek(message);
                    }
                    continue;

                case EMBED:
                    if (ss.isAtEnd()) {
                        ss = null;
                        state = State.TOP;
                        continue;
                    }
                    int subKind = ss.readRawVarint32();
                    int subSize = ss.readRawVarint32();
                    byte[] subData = ss.readRawBytes(subSize);
                    Class<? extends GeneratedMessage> subClazz = PacketTypes.EMBED.get(subKind);
                    if (subClazz == null) {
                        log.warn("unknown embedded message of kind {}", subKind);
                        continue;
                    }
                    GeneratedMessage subMessage = PacketTypes.parse(subClazz, subData);
                    if (subMessage instanceof CNETMsg_Tick) {
                        tick = ((CNETMsg_Tick) subMessage).getTick();
                        border = border.addNetTickBorder();
                        if (!isFiltered(CNETMsg_Tick.class)) {
                            return genPeek(subMessage);
                        }
                    } else if (subMessage instanceof CSVCMsg_UserMessage) {
                        if (!isFiltered(CSVCMsg_UserMessage.class)) {
                            CSVCMsg_UserMessage userMessage = (CSVCMsg_UserMessage) subMessage;
                            Class<? extends GeneratedMessage> umClazz = PacketTypes.USERMSG.get(userMessage.getMsgType());
                            if (umClazz == null) {
                                log.warn("unknown usermessage of kind {}", userMessage.getMsgType());
                                continue;
                            } else if (!isFiltered(umClazz)) {
                                return genPeek(PacketTypes.parse(umClazz, userMessage.getMsgData().toByteArray()));
                            }
                        }
                    } else if (!isFiltered(subMessage.getClass())) {
                        return genPeek(subMessage);
                    }
                    continue;
            }
        }
        return null;
    }    
    
    public void close() throws IOException {
      is.close();
    }
}
