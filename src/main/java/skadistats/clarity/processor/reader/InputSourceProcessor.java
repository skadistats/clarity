package skadistats.clarity.processor.reader;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.ZeroCopy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xerial.snappy.Snappy;
import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.Initializer;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.LoopController;
import skadistats.clarity.processor.runner.OnInputSource;
import skadistats.clarity.source.Source;
import skadistats.clarity.util.Predicate;
import skadistats.clarity.wire.Packet;
import skadistats.clarity.wire.common.DemoPackets;
import skadistats.clarity.wire.common.proto.Demo;
import skadistats.clarity.wire.common.proto.NetMessages;
import skadistats.clarity.wire.common.proto.NetworkBaseTypes;

import java.io.EOFException;
import java.io.IOException;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Provides({OnMessageContainer.class, OnMessage.class, OnTickStart.class, OnTickEnd.class, OnReset.class, OnFullPacket.class })
public class InputSourceProcessor {

    private static final Logger log = LoggerFactory.getLogger(InputSourceProcessor.class);

    private final byte[][] buffer = new byte[][] { new byte[512*1024], new byte[512*1024], new byte[512*1024] };

    // TODO: set to false when issue #58 is closed.
    private boolean unpackUserMessages = true;

    private Event<OnReset> evReset = null;
    private Event<OnFullPacket> evFull = null;

    @Initializer(OnMessage.class)
    public void initOnMessageListener(final Context ctx, final EventListener<OnMessage> listener) {
        listener.setParameterClasses(listener.getAnnotation().value());
        unpackUserMessages |= ctx.getEngineType().isUserMessage(listener.getAnnotation().value());
    }

    @Initializer(OnReset.class)
    public void initOnResetListener(final Context ctx, final EventListener<OnReset> listener) {
        evReset = ctx.createEvent(OnReset.class, Demo.CDemoStringTables.class, ResetPhase.class);
    }

    @Initializer(OnFullPacket.class)
    public void initOnFullPacketListener(final Context ctx, final EventListener<OnFullPacket> listener) {
        evFull = ctx.createEvent(OnFullPacket.class, Demo.CDemoFullPacket.class);
    }

    @Initializer(OnMessageContainer.class)
    public void initOnMessageContainerListener(final Context ctx, final EventListener<OnMessageContainer> listener) {
        listener.setInvocationPredicate(new Predicate<Object[]>() {
            @Override
            public boolean apply(Object[] args) {
                Class<? extends GeneratedMessage> clazz = (Class<? extends GeneratedMessage>) args[0];
                return listener.getAnnotation().value().isAssignableFrom(clazz);
            }
        });
    }

    private ByteString readPacket(Source source, int size, boolean isCompressed) throws IOException {
        source.readBytes(buffer[0], 0, size);
        if (isCompressed) {
            int sizeUncompressed = Snappy.rawUncompress(buffer[0], 0, size, buffer[1], 0);
            return ZeroCopy.wrapBounded(buffer[1], 0, sizeUncompressed);
        } else {
            return ZeroCopy.wrapBounded(buffer[0], 0, size);
        }
    }

    private void logUnknownMessage(Context ctx, String where, int type) {
        log.warn("unknown {} message of kind {}/{}. Please report this in the corresponding issue: https://github.com/skadistats/clarity/issues/58", where, ctx.getEngineType(), type);
    }

    @OnInputSource
    public void processSource(Context ctx, Source src, LoopController ctl) throws IOException {
        int compressedFlag = ctx.getEngineType().getCompressedFlag();
        outer: while (true) {
            int offset = src.getPosition();
            int kind = 0;
            try {
                kind = src.readVarInt32();
            } catch (EOFException e) {
                LoopController.Command loopCtl = ctl.doLoopControl(ctx, Integer.MAX_VALUE);
                if (loopCtl == LoopController.Command.FALLTHROUGH) {
                    continue;
                } else if (loopCtl == LoopController.Command.CONTINUE) {
                    continue;
                } else if (loopCtl == LoopController.Command.BREAK) {
                    break outer;
                } else if (loopCtl == LoopController.Command.RESET_COMPLETE) {
                    if (evReset != null) {
                        evReset.raise(null, ResetPhase.COMPLETE);
                    }
                    continue;
                }
            }
            boolean isCompressed = (kind & compressedFlag) == compressedFlag;
            kind &= ~compressedFlag;
            int tick = src.readVarInt32();
            int size = src.readVarInt32();
            while (true) {
                LoopController.Command loopCtl = ctl.doLoopControl(ctx, tick);
                if (loopCtl == LoopController.Command.FALLTHROUGH) {
                    break;
                } else if (loopCtl == LoopController.Command.CONTINUE) {
                    continue outer;
                } else if (loopCtl == LoopController.Command.BREAK) {
                    break outer;
                } else if (loopCtl == LoopController.Command.RESET_COMPLETE) {
                    if (evReset != null) {
                        evReset.raise(null, ResetPhase.COMPLETE);
                    }
                }
            }
            Class<? extends GeneratedMessage> messageClass = DemoPackets.classForKind(kind);
            Demo.CDemoFullPacket fullPacket = null;
            Demo.CDemoStringTables stringTables = null;
            if (messageClass == null) {
                logUnknownMessage(ctx, "top level", kind);
                src.skipBytes(size);
            } else if (messageClass == Demo.CDemoPacket.class) {
                Demo.CDemoPacket message = (Demo.CDemoPacket) Packet.parse(messageClass, readPacket(src, size, isCompressed));
                ctx.createEvent(OnMessageContainer.class, Class.class, ByteString.class).raise(Demo.CDemoPacket.class, message.getData());
            } else if (ctx.getEngineType().isSendTablesContainer() && messageClass == Demo.CDemoSendTables.class) {
                Demo.CDemoSendTables message = (Demo.CDemoSendTables) Packet.parse(messageClass, readPacket(src, size, isCompressed));
                ctx.createEvent(OnMessageContainer.class, Class.class, ByteString.class).raise(Demo.CDemoSendTables.class, message.getData());
            } else if (messageClass == Demo.CDemoFullPacket.class) {
                if (evFull != null || evReset != null) {
                    fullPacket = (Demo.CDemoFullPacket) Packet.parse(messageClass, readPacket(src, size, isCompressed));
                } else {
                    src.skipBytes(size);
                }
                if (evFull != null) {
                    evFull.raise(fullPacket);
                }
                if (evReset != null) {
                    ctl.markCDemoStringTables(tick, offset);
                    stringTables = fullPacket.getStringTable();
                }
            } else {
                Event<OnMessage> ev = ctx.createEvent(OnMessage.class, messageClass);
                boolean markStringTables = evReset != null && messageClass == Demo.CDemoStringTables.class;
                GeneratedMessage message = null;
                if (ev.isListenedTo() || markStringTables) {
                    message = Packet.parse(messageClass, readPacket(src, size, isCompressed));
                } else {
                    src.skipBytes(size);
                }
                if (ev.isListenedTo()) {
                    ev.raise(message);
                }
                if (markStringTables) {
                    ctl.markCDemoStringTables(tick, offset);
                    stringTables = (Demo.CDemoStringTables) message;
                }
            }
            if (stringTables != null) {
                Iterator<ResetPhase> phases = ctl.evaluateResetPhases();
                if (evReset != null && phases.hasNext()) {
                    ResetPhase phase = null;
                    while (phases.hasNext()) {
                        phase = phases.next();
                        evReset.raise(stringTables, phase);
                    }
                    if (phase == ResetPhase.STRINGTABLE_APPLY && fullPacket != null) {
                        ctx.createEvent(OnMessageContainer.class, Class.class, ByteString.class).raise(Demo.CDemoFullPacket.class, fullPacket.getPacket().getData());
                    }
                }
            }
        }
    }

    @OnMessageContainer
    public void processEmbedded(Context ctx, Class<? extends GeneratedMessage> containerClass, ByteString bytes) throws IOException {
        BitStream bs = BitStream.createBitStream(bytes);
        while (bs.remaining() >= 8) {
            int kind = ctx.getEngineType().readEmbeddedKind(bs);
            if (kind == 0) {
                // this seems to happen with console recorded replays
                break;
            }
            int size = bs.readVarUInt();
            Class<? extends GeneratedMessage> messageClass = ctx.getEngineType().embeddedPacketClassForKind(kind);
            if (messageClass == null) {
                logUnknownMessage(ctx, "embedded", kind);
                bs.skip(size * 8);
            } else {
                Event<OnMessage> ev = ctx.createEvent(OnMessage.class, messageClass);
                if (ev.isListenedTo() || (unpackUserMessages && messageClass == NetworkBaseTypes.CSVCMsg_UserMessage.class)) {
                    bs.readBitsIntoByteArray(buffer[2], size * 8);
                    GeneratedMessage subMessage = Packet.parse(messageClass, ZeroCopy.wrapBounded(buffer[2], 0, size));
                    if (ev.isListenedTo()) {
                        ev.raise(subMessage);
                    }
                    if (unpackUserMessages && messageClass == NetworkBaseTypes.CSVCMsg_UserMessage.class) {
                        NetworkBaseTypes.CSVCMsg_UserMessage userMessage = (NetworkBaseTypes.CSVCMsg_UserMessage) subMessage;
                        Class<? extends GeneratedMessage> umClazz = ctx.getEngineType().userMessagePacketClassForKind(userMessage.getMsgType());
                        if (umClazz == null) {
                            logUnknownMessage(ctx, "usermessage", userMessage.getMsgType());
                        } else {
                            ctx.createEvent(OnMessage.class, umClazz).raise(Packet.parse(umClazz, userMessage.getMsgData()));
                        }
                    }
                } else {
                    bs.skip(size * 8);
                }
            }
        }
    }

    @OnMessage(NetMessages.CSVCMsg_ServerInfo.class)
    public void processServerInfo(Context ctx, NetMessages.CSVCMsg_ServerInfo serverInfo) {
        if (ctx.getEngineType() == EngineType.SOURCE1) {
            return;
        }
        Matcher matcher = Pattern.compile("dota_v(\\d+)").matcher(serverInfo.getGameDir());
        if (matcher.find()) {
            int num = Integer.valueOf(matcher.group(1));
            ctx.setBuildNumber(num);
            if (num < 928) {
                log.warn("This replay is from an early beta version of Dota 2 Reborn (build number {}).", ctx.getBuildNumber());
                log.warn("Entities in this replay probably cannot be read.");
                log.warn("However, I have not had the opportunity to analyze a replay with that build number.");
                log.warn("If you wanna help, send it to clarity@martin.schrodt.org, or contact me on github.");
            }
        } else {
            log.warn("received CSVCMsg_ServerInfo, but could not read build number from it. (game dir '{}')", serverInfo.getGameDir());
        }
    }

}
