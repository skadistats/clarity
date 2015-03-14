package skadistats.clarity.two.processor.reader;

import com.dota2.proto.Demo;
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

@Provides({OnMessageContainer.class, OnMessage.class, OnFileInfoOffset.class})
public class InputStreamProcessor {

    private static final Logger log = LoggerFactory.getLogger(InputStreamProcessor.class);

    @Initializer(OnMessage.class)
    public void initOnMessageListener(final Context ctx, final EventListener<OnMessage> listener) {
        listener.setParameterClasses(listener.getAnnotation().value());
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
        CodedInputStream cs = CodedInputStream.newInstance(is);
        cs.setSizeLimit(Integer.MAX_VALUE);
        String header = new String(cs.readRawBytes(8));
        if (!"PBUFDEM\0".equals(header)) {
            throw new IOException("replay does not have the proper header");
        }
        ctx.createEvent(OnFileInfoOffset.class, int.class).raise(cs.readFixed32());

        while (!cs.isAtEnd()) {
            int kind = cs.readRawVarint32();
            boolean isCompressed = (kind & Demo.EDemoCommands.DEM_IsCompressed_VALUE) == Demo.EDemoCommands.DEM_IsCompressed_VALUE;
            kind &= ~Demo.EDemoCommands.DEM_IsCompressed_VALUE;
            int tick = cs.readRawVarint32();
            int size = cs.readRawVarint32();
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
                if (ev.isListenedTo()) {
                    GeneratedMessage subMessage = PacketTypes.parse(messageClass, cs.readRawBytes(size));
                    ev.raise(subMessage);
                } else {
                    cs.skipRawBytes(size);
                }
            }
        }
    }
}
