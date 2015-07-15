package skadistats.clarity.parser;

import com.google.protobuf.CodedInputStream;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.ZeroCopy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;
import skadistats.clarity.parser.Peek.BorderType;
import skadistats.clarity.wire.s1.PacketTypes;
import skadistats.clarity.wire.s1.proto.Demo.*;
import skadistats.clarity.wire.s1.proto.Networkbasetypes.CNETMsg_Tick;
import skadistats.clarity.wire.s1.proto.Networkbasetypes.CSVCMsg_UserMessage;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class DemoInputStream implements Closeable {

    private enum State {
        TOP, EMBED
    };

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Profile[] profile;
    private final InputStream is; // source stream
    private final CodedInputStream ms; // main stream
    private CodedInputStream es = null; // stream for embedded packet
    private int fileInfoOffset;
    private int n = -1;
    private int tick = 0;
    private int peekTick = 0;
    private boolean full = false;
    private BorderType border = BorderType.NONE;
    private State state = State.TOP;

    public DemoInputStream(InputStream is, Profile... profile) {
    	this.is = is;
    	this.ms = CodedInputStream.newInstance(is);
        this.profile = profile;
    }
    
    public void bootstrap() throws IOException {
        ms.setSizeLimit(Integer.MAX_VALUE);
        String header = new String(ms.readRawBytes(8));
        if (!"PBUFDEM\0".equals(header)) {
            throw new IOException("replay does not have the proper header");
        }
        fileInfoOffset = ms.readFixed32();
    }
    
    public void skipToFileInfo() throws IOException {
    	ms.skipRawBytes(fileInfoOffset - 12);
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
        while (!ms.isAtEnd()) {
            switch (state) {
                case TOP:
                    int kind = ms.readRawVarint32();
                    boolean isCompressed = (kind & EDemoCommands.DEM_IsCompressed_VALUE) == EDemoCommands.DEM_IsCompressed_VALUE;
                    kind &= ~EDemoCommands.DEM_IsCompressed_VALUE;
                    int nextPeekTick = ms.readRawVarint32();
                    if (nextPeekTick != peekTick) {
                        border = border.addPeekTickBorder();
                    }
                    peekTick = nextPeekTick;
                    int size = ms.readRawVarint32();
                    byte[] data = ms.readRawBytes(size);
                    if (isCompressed) {
                    	if (Snappy.isValidCompressedBuffer(data)) {
                            data = Snappy.uncompress(data);
                    	} else {
                    		throw new IOException("according to snappy, the compressed packet is not valid!");
                    	}
                    }
                    Class<? extends GeneratedMessage> topClazz = PacketTypes.DEMO.get(kind);
                    if (topClazz == null) {
                        log.warn("unknown top level message of kind {}", kind);
                        continue;
                    }
                    GeneratedMessage message = PacketTypes.parse(topClazz, ZeroCopy.wrap(data));
                    full = false;
                    if (message instanceof CDemoPacket) {
                        es = ((CDemoPacket) message).getData().newCodedInput();
                        state = State.EMBED;
                        continue;
                    } else if (message instanceof CDemoSendTables) {
                        es = ((CDemoSendTables) message).getData().newCodedInput();
                        state = State.EMBED;
                        continue;
                    } else if (message instanceof CDemoFullPacket) {
                        CDemoFullPacket fullMessage = (CDemoFullPacket)message;
                        es = fullMessage.getPacket().getData().newCodedInput();
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
                    if (es.isAtEnd()) {
                        es = null;
                        state = State.TOP;
                        continue;
                    }
                    int subKind = es.readRawVarint32();
                    int subSize = es.readRawVarint32();
                    byte[] subData = es.readRawBytes(subSize);
                    Class<? extends GeneratedMessage> subClazz = PacketTypes.EMBED.get(subKind);
                    if (subClazz == null) {
                        log.warn("unknown embedded message of kind {}", subKind);
                        continue;
                    }
                    GeneratedMessage subMessage = PacketTypes.parse(subClazz, ZeroCopy.wrap(subData));
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
                                return genPeek(PacketTypes.parse(umClazz, userMessage.getMsgData()));
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

	@Override
	public void close() throws IOException {
		is.close();
	}

}
