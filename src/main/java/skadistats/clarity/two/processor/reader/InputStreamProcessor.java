package skadistats.clarity.two.processor.reader;

import com.dota2.proto.Demo;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;
import skadistats.clarity.parser.PacketTypes;
import skadistats.clarity.two.framework.EnlistmentMode;
import skadistats.clarity.two.framework.EventListener;
import skadistats.clarity.two.framework.annotation.Initializer;
import skadistats.clarity.two.framework.annotation.ProvidesEvent;
import skadistats.clarity.two.processor.reader.event.OnFileInfoOffset;
import skadistats.clarity.two.processor.reader.event.OnInputStream;
import skadistats.clarity.two.processor.reader.event.OnMessage;
import skadistats.clarity.two.processor.reader.event.OnMessageContainer;
import skadistats.clarity.two.runner.Context;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

@ProvidesEvent({OnMessageContainer.class, OnMessage.class, OnFileInfoOffset.class})
public class InputStreamProcessor {

    private static final Logger log = LoggerFactory.getLogger(InputStreamProcessor.class);

    private Set<Class<? extends GeneratedMessage>> raisedClasses = new HashSet<>();

    @Initializer(OnMessage.class)
    public void onMessageEventHandlerFound(Context ctx, EventListener listener) {
        OnMessage a = (OnMessage) listener.getAnnotation();
        raisedClasses.add(a.value());
    }

    private boolean isEmitted(Class<? extends GeneratedMessage> messageClass) {
        return raisedClasses.contains(messageClass);
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
        ctx.raise(OnFileInfoOffset.class, cs.readFixed32());

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
                ctx.raise(OnMessageContainer.class, CodedInputStream.newInstance(message.getData().toByteArray()));
            } else if (messageClass == Demo.CDemoSendTables.class) {
                Demo.CDemoSendTables message = (Demo.CDemoSendTables) PacketTypes.parse(messageClass, readData(cs, size, isCompressed));
                ctx.raise(OnMessageContainer.class, CodedInputStream.newInstance(message.getData().toByteArray()));
            } else if (messageClass == Demo.CDemoFullPacket.class) {
                // TODO: ignored for now
                cs.skipRawBytes(size);
            } else if (isEmitted(messageClass)) {
                GeneratedMessage message = PacketTypes.parse(messageClass, readData(cs, size, isCompressed));
                ctx.raise(OnMessage.class, message);
            } else {
                cs.skipRawBytes(size);
            }
        }
    }

    @OnMessageContainer(enlist = EnlistmentMode.NONE)
    public void processEmbedded(Context ctx, CodedInputStream cs) throws IOException {
        while (!cs.isAtEnd()) {
            int kind = cs.readRawVarint32();
            int size = cs.readRawVarint32();
            Class<? extends GeneratedMessage> messageClass = PacketTypes.EMBED.get(kind);
            if (messageClass == null) {
                log.warn("unknown embedded message of kind {}", kind);
                cs.skipRawBytes(size);
            } else if (isEmitted(messageClass)) {
                GeneratedMessage subMessage = PacketTypes.parse(messageClass, cs.readRawBytes(size));
                ctx.raise(OnMessage.class, subMessage);
            } else {
                cs.skipRawBytes(size);
            }
        }
    }
}
