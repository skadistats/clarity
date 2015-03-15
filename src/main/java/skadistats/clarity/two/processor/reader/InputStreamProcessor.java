package skadistats.clarity.two.processor.reader;

import com.dota2.proto.Demo;
import com.dota2.proto.Networkbasetypes;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;
import skadistats.clarity.parser.PacketTypes;
import skadistats.clarity.two.framework.annotation.Initializer;
import skadistats.clarity.two.framework.annotation.Provides;
import skadistats.clarity.two.framework.invocation.Event;
import skadistats.clarity.two.framework.invocation.EventListener;
import skadistats.clarity.two.processor.runner.Context;
import skadistats.clarity.two.processor.runner.OnInputStream;

import java.io.IOException;
import java.io.InputStream;

@Provides({OnMessageContainer.class, OnMessage.class, OnFileInfoOffset.class, OnTickStart.class, OnTickEnd.class })
public class InputStreamProcessor {

    private static final Logger log = LoggerFactory.getLogger(InputStreamProcessor.class);

    private CodedInputStream mainStream;
    private boolean unpackUserMessages = false;

    @Initializer(OnMessage.class)
    public void initOnMessageListener(final Context ctx, final EventListener<OnMessage> listener) {
        listener.setParameterClasses(listener.getAnnotation().value());
        unpackUserMessages |= PacketTypes.USERMSG.containsValue(listener.getAnnotation().value());
    }

    private byte[] readData(CodedInputStream ms, int size, boolean isCompressed) throws IOException {
        byte[] data = ms.readRawBytes(size);
        if (isCompressed) {
            if (Snappy.isValidCompressedBuffer(data)) {
                data = Snappy.uncompress(data);
            } else {
                throw new IOException("according to snappy, the compressed packet is not valid!");
            }
        }
        return data;
    }

    @OnInputStream
    public void processStream(Context ctx, InputStream is) throws IOException {
        mainStream = CodedInputStream.newInstance(is);
        CodedInputStream cs = mainStream;
        cs.setSizeLimit(Integer.MAX_VALUE);
        String header = new String(cs.readRawBytes(8));
        if (!"PBUFDEM\0".equals(header)) {
            throw new IOException("replay does not have the proper header");
        }
        ctx.createEvent(OnFileInfoOffset.class, int.class).raise(cs.readFixed32());
        ctx.createEvent(OnTickStart.class).raise();
        while (!cs.isAtEnd()) {
            int kind = cs.readRawVarint32();
            boolean isCompressed = (kind & Demo.EDemoCommands.DEM_IsCompressed_VALUE) == Demo.EDemoCommands.DEM_IsCompressed_VALUE;
            kind &= ~Demo.EDemoCommands.DEM_IsCompressed_VALUE;
            int tick = cs.readRawVarint32();
            int size = cs.readRawVarint32();
            boolean newTick = tick != ctx.getTick();
            if (newTick) {
                ctx.createEvent(OnTickEnd.class).raise();
                ctx.setTick(tick);
                ctx.createEvent(OnTickStart.class).raise();
            }
            Class<? extends GeneratedMessage> messageClass = PacketTypes.DEMO.get(kind);
            if (messageClass == null) {
                log.warn("unknown top level message of kind {}", kind);
                cs.skipRawBytes(size);
            } else if (messageClass == Demo.CDemoPacket.class) {
                Demo.CDemoPacket message = (Demo.CDemoPacket) PacketTypes.parse(messageClass, readData(cs, size, isCompressed));
                ctx.createEvent(OnMessageContainer.class, CodedInputStream.class).raise(CodedInputStream.newInstance(message.getData().toByteArray()));
            } else if (messageClass == Demo.CDemoSendTables.class) {
                Demo.CDemoSendTables message = (Demo.CDemoSendTables) PacketTypes.parse(messageClass, readData(cs, size, isCompressed));
                ctx.createEvent(OnMessageContainer.class, CodedInputStream.class).raise(CodedInputStream.newInstance(message.getData().toByteArray()));
            } else if (messageClass == Demo.CDemoFullPacket.class) {
                // TODO: ignored for now
                cs.skipRawBytes(size);
            } else {
                Event<OnMessage> ev = ctx.createEvent(OnMessage.class, messageClass);
                if (ev.isListenedTo()) {
                    GeneratedMessage message = PacketTypes.parse(messageClass, readData(cs, size, isCompressed));
                    ev.raise(message);
                } else {
                    cs.skipRawBytes(size);
                }
            }
        }
        ctx.createEvent(OnTickEnd.class).raise();
    }

    @OnMessageContainer
    public void processEmbedded(Context ctx, CodedInputStream cs) throws IOException {
        while (!cs.isAtEnd()) {
            int kind = cs.readRawVarint32();
            int size = cs.readRawVarint32();
            Class<? extends GeneratedMessage> messageClass = PacketTypes.EMBED.get(kind);
            if (messageClass == null) {
                log.warn("unknown embedded message of kind {}", kind);
                cs.skipRawBytes(size);
            } else {
                Event<OnMessage> ev = ctx.createEvent(OnMessage.class, messageClass);
                if (ev.isListenedTo() || (unpackUserMessages && messageClass == Networkbasetypes.CSVCMsg_UserMessage.class)) {
                    GeneratedMessage subMessage = PacketTypes.parse(messageClass, cs.readRawBytes(size));
                    if (ev.isListenedTo()) {
                        ev.raise(subMessage);
                    }
                    if (unpackUserMessages && messageClass == Networkbasetypes.CSVCMsg_UserMessage.class) {
                        Networkbasetypes.CSVCMsg_UserMessage userMessage = (Networkbasetypes.CSVCMsg_UserMessage) subMessage;
                        Class<? extends GeneratedMessage> umClazz = PacketTypes.USERMSG.get(userMessage.getMsgType());
                        if (umClazz == null) {
                            log.warn("unknown usermessage of kind {}", userMessage.getMsgType());
                        } else {
                            ctx.createEvent(OnMessage.class, umClazz).raise(PacketTypes.parse(umClazz, userMessage.getMsgData().toByteArray()));
                        }
                    }
                } else {
                    cs.skipRawBytes(size);
                }
            }
        }
    }

    public void skipBytes(int numBytes) throws IOException {
        mainStream.skipRawBytes(numBytes);
    }

}
