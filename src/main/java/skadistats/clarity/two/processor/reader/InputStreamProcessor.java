package skadistats.clarity.two.processor.reader;

import com.dota2.proto.Demo;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.GeneratedMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;
import skadistats.clarity.parser.PacketTypes;
import skadistats.clarity.two.framework.annotation.Initializer;
import skadistats.clarity.two.framework.annotation.ProvidesEvent;
import skadistats.clarity.two.processor.reader.event.OnFileInfoOffset;
import skadistats.clarity.two.processor.reader.event.OnInputStream;
import skadistats.clarity.two.processor.reader.event.OnMessage;
import skadistats.clarity.two.processor.reader.event.OnMessageContainer;
import skadistats.clarity.two.runner.Context;

import java.io.IOException;
import java.io.InputStream;

@ProvidesEvent({OnMessageContainer.class, OnMessage.class, OnFileInfoOffset.class})
public class InputStreamProcessor {

    private static final Logger log = LoggerFactory.getLogger(InputStreamProcessor.class);

    @Initializer(OnMessage.class)
    public void onMessageEventHandlerFound() {
        System.out.println("INIT ON MESSAGE");
    }

    @OnInputStream
    public void readStream(Context ctx, InputStream is) throws IOException {
        CodedInputStream ms = CodedInputStream.newInstance(is);
        ms.setSizeLimit(Integer.MAX_VALUE);
        String header = new String(ms.readRawBytes(8));
        if (!"PBUFDEM\0".equals(header)) {
            throw new IOException("replay does not have the proper header");
        }
        ctx.raise(OnFileInfoOffset.class, ms.readFixed32());

        while (!ms.isAtEnd()) {
            int kind = ms.readRawVarint32();
            boolean isCompressed = (kind & Demo.EDemoCommands.DEM_IsCompressed_VALUE) == Demo.EDemoCommands.DEM_IsCompressed_VALUE;
            kind &= ~Demo.EDemoCommands.DEM_IsCompressed_VALUE;
            int tick = ms.readRawVarint32();
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
            if (topClazz == Demo.CDemoPacket.class) {
                Demo.CDemoPacket message = (Demo.CDemoPacket) PacketTypes.parse(topClazz, data);
                ctx.raise(OnMessageContainer.class, CodedInputStream.newInstance(message.getData().toByteArray()));
            } else if (topClazz == Demo.CDemoSendTables.class) {
                Demo.CDemoSendTables message = (Demo.CDemoSendTables) PacketTypes.parse(topClazz, data);
                ctx.raise(OnMessageContainer.class, CodedInputStream.newInstance(message.getData().toByteArray()));
            } else if (topClazz == Demo.CDemoFullPacket.class) {
            } else {
                GeneratedMessage message = PacketTypes.parse(topClazz, data);
                ctx.raise(OnMessage.class, message);
            }
        }
    }

    @OnMessageContainer
    public void unpackEmbedded(Context ctx, CodedInputStream es) throws IOException {
        while (!es.isAtEnd()) {
            int subKind = es.readRawVarint32();
            int subSize = es.readRawVarint32();
            byte[] subData = es.readRawBytes(subSize);
            Class<? extends GeneratedMessage> subClazz = PacketTypes.EMBED.get(subKind);
            if (subClazz == null) {
                log.warn("unknown embedded message of kind {}", subKind);
                continue;
            }
            GeneratedMessage subMessage = PacketTypes.parse(subClazz, subData);
            ctx.raise(OnMessage.class, subMessage);
        }

        //log.info("unpack {}", es.getClass().getName());
    }
}
