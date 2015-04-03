package skadistats.clarity.processor.reader;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.ZeroCopy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.Initializer;
import skadistats.clarity.event.Provides;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.OnInputSource;
import skadistats.clarity.processor.runner.Source;
import skadistats.clarity.wire.PacketTypes;
import skadistats.clarity.wire.proto.Demo;
import skadistats.clarity.wire.proto.Networkbasetypes;

import java.io.IOException;

@Provides({OnMessageContainer.class, OnMessage.class, OnTickStart.class, OnTickEnd.class })
public class InputSourceProcessor {

    private static final Logger log = LoggerFactory.getLogger(InputSourceProcessor.class);

    private boolean unpackUserMessages = false;

    @Initializer(OnMessage.class)
    public void initOnMessageListener(final Context ctx, final EventListener<OnMessage> listener) {
        listener.setParameterClasses(listener.getAnnotation().value());
        unpackUserMessages |= PacketTypes.USERMSG.containsValue(listener.getAnnotation().value());
    }

    private ByteString readPacket(Source source, int size, boolean isCompressed) throws IOException {
        byte[] data = source.stream().readRawBytes(size);
        if (isCompressed) {
            data = Snappy.uncompress(data);
        }
        return ZeroCopy.wrap(data);
    }

    @OnInputSource
    public void processSource(Context ctx, Source src) throws IOException {
        while (true) {
            if (src.stream().isAtEnd()) {
                Source.LoopControlCommand loopCtl = src.doLoopControl(ctx, Integer.MAX_VALUE);
                if (loopCtl == Source.LoopControlCommand.CONTINUE) {
                    continue;
                } else {
                    // FALLTHROUGH at end of stream means to break also.
                    break;
                }
            } else {
                int kind = src.stream().readRawVarint32();
                boolean isCompressed = (kind & Demo.EDemoCommands.DEM_IsCompressed_VALUE) == Demo.EDemoCommands.DEM_IsCompressed_VALUE;
                kind &= ~Demo.EDemoCommands.DEM_IsCompressed_VALUE;
                int tick = src.stream().readRawVarint32();
                int size = src.stream().readRawVarint32();
                if (src.isTickBorder(tick)) {
                    Source.LoopControlCommand loopCtl = src.doLoopControl(ctx, tick);
                    if (loopCtl == Source.LoopControlCommand.CONTINUE) {
                        continue;
                    } else if (loopCtl == Source.LoopControlCommand.BREAK) {
                        break;
                    }
                }
                Class<? extends GeneratedMessage> messageClass = PacketTypes.DEMO.get(kind);
                if (messageClass == null) {
                    log.warn("unknown top level message of kind {}", kind);
                    src.stream().skipRawBytes(size);
                } else if (messageClass == Demo.CDemoPacket.class) {
                    Demo.CDemoPacket message = (Demo.CDemoPacket) PacketTypes.parse(messageClass, readPacket(src, size, isCompressed));
                    ctx.createEvent(OnMessageContainer.class, CodedInputStream.class).raise(message.getData().newCodedInput());
                } else if (messageClass == Demo.CDemoSendTables.class) {
                    Demo.CDemoSendTables message = (Demo.CDemoSendTables) PacketTypes.parse(messageClass, readPacket(src, size, isCompressed));
                    ctx.createEvent(OnMessageContainer.class, CodedInputStream.class).raise(message.getData().newCodedInput());
                } else if (messageClass == Demo.CDemoFullPacket.class) {
                    src.markFullPacket(tick, size, isCompressed);
                    src.stream().skipRawBytes(size);
                } else {
                    Event<OnMessage> ev = ctx.createEvent(OnMessage.class, messageClass);
                    if (ev.isListenedTo()) {
                        GeneratedMessage message = PacketTypes.parse(messageClass, readPacket(src, size, isCompressed));
                        ev.raise(message);
                    } else {
                        src.stream().skipRawBytes(size);
                    }
                }
            }
        }
    }

    @OnMessageContainer
    public void processEmbedded(Context ctx, CodedInputStream cs) throws IOException {
        while (!cs.isAtEnd()) {
            int kind = cs.readRawVarint32();
            if (kind == 0) {
                // this seems to happen with console recorded replays
                break;
            }
            int size = cs.readRawVarint32();
            Class<? extends GeneratedMessage> messageClass = PacketTypes.EMBED.get(kind);
            if (messageClass == null) {
                log.warn("unknown embedded message of kind {}", kind);
                cs.skipRawBytes(size);
            } else {
                Event<OnMessage> ev = ctx.createEvent(OnMessage.class, messageClass);
                if (ev.isListenedTo() || (unpackUserMessages && messageClass == Networkbasetypes.CSVCMsg_UserMessage.class)) {
                    GeneratedMessage subMessage = PacketTypes.parse(messageClass, ZeroCopy.wrap(cs.readRawBytes(size)));
                    if (ev.isListenedTo()) {
                        ev.raise(subMessage);
                    }
                    if (unpackUserMessages && messageClass == Networkbasetypes.CSVCMsg_UserMessage.class) {
                        Networkbasetypes.CSVCMsg_UserMessage userMessage = (Networkbasetypes.CSVCMsg_UserMessage) subMessage;
                        Class<? extends GeneratedMessage> umClazz = PacketTypes.USERMSG.get(userMessage.getMsgType());
                        if (umClazz == null) {
                            log.warn("unknown usermessage of kind {}", userMessage.getMsgType());
                        } else {
                            ctx.createEvent(OnMessage.class, umClazz).raise(PacketTypes.parse(umClazz, userMessage.getMsgData()));
                        }
                    }
                } else {
                    cs.skipRawBytes(size);
                }
            }
        }
    }

}
