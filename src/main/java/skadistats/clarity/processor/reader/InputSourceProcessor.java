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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Provides({OnMessageContainer.class, OnMessage.class, OnTickStart.class, OnTickEnd.class, OnReset.class, OnFullPacket.class })
public class InputSourceProcessor {

    private static final Logger log = LoggerFactory.getLogger(InputSourceProcessor.class);

    private final byte[][] buffer = new byte[][] { new byte[128*1024], new byte[256*1024], new byte[128*1024] };

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

    private void ensureBufferCapacity(int n, int capacity) {
        if (buffer[n].length < capacity) {
            buffer[n] = new byte[capacity];
        }
    }

    private ByteString readPacket(Source source, int size, boolean isCompressed) throws IOException {
        ensureBufferCapacity(0, size);
        source.readBytes(buffer[0], 0, size);
        if (isCompressed) {
            int sizeUncompressed = Snappy.uncompressedLength(buffer[0], 0, size);
            ensureBufferCapacity(1, sizeUncompressed);
            Snappy.rawUncompress(buffer[0], 0, size, buffer[1], 0);
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

        ByteString resetFullPacketData = null;

        int offset;
        int kind;
        boolean isCompressed;
        int tick;
        int size;
        LoopController.Command loopCtl;

        main: while (true) {
            offset = src.getPosition();
            try {
                kind = src.readVarInt32();
                isCompressed = (kind & compressedFlag) == compressedFlag;
                kind &= ~compressedFlag;
                tick = src.readVarInt32();
                size = src.readVarInt32();
            } catch (EOFException e) {
                kind = -1;
                isCompressed = false;
                tick = Integer.MAX_VALUE;
                size = 0;
            }
            loopctl: while (true) {
                loopCtl = ctl.doLoopControl(ctx, tick);
                switch(loopCtl) {
                    case RESET_CLEAR:
                        if (evReset != null) {
                            evReset.raise(null, ResetPhase.CLEAR);
                        }
                        continue loopctl;
                    case RESET_ACCUMULATE:
                        // accumulate a single string table change
                        break loopctl;
                    case RESET_APPLY:
                        if (evReset != null) {
                            // apply string table changes
                            evReset.raise(null, ResetPhase.APPLY);
                        }
                        if (resetFullPacketData != null) {
                            // apply full packet for entities
                            ctx.createEvent(OnMessageContainer.class, Class.class, ByteString.class).raise(Demo.CDemoFullPacket.class, resetFullPacketData);
                            resetFullPacketData = null;
                        }
                        continue loopctl;
                    case RESET_COMPLETE:
                        if (evReset != null) {
                            evReset.raise(null, ResetPhase.COMPLETE);
                        }
                        continue loopctl;
                    case FALLTHROUGH:
                        if (tick != Integer.MAX_VALUE) {
                            break loopctl;
                        }
                        // if at end, fallthrough is a break from main
                    case BREAK:
                        break main;
                    case CONTINUE:
                        continue main;
                    case AGAIN:
                        continue loopctl;
                }
            }
            if (size < 0) {
                throw new IOException(String.format("invalid negative demo packet size (%d).", size));
            }
            Class<? extends GeneratedMessage> messageClass = DemoPackets.classForKind(kind);
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
                    Demo.CDemoFullPacket message = (Demo.CDemoFullPacket) Packet.parse(messageClass, readPacket(src, size, isCompressed));
                    if (evFull != null) {
                        evFull.raise(message);
                    }
                    if (evReset != null) {
                        ctl.markResetRelevantPacket(tick, kind, offset);
                        switch (loopCtl) {
                            case RESET_ACCUMULATE:
                                evReset.raise(message.getStringTable(), ResetPhase.ACCUMULATE);
                                resetFullPacketData = message.getPacket().getData();
                                break;
                        }
                    }
                } else {
                    src.skipBytes(size);
                }
            } else {
                boolean isStringTables = messageClass == Demo.CDemoStringTables.class;
                boolean isSyncTick = messageClass == Demo.CDemoSyncTick.class;
                boolean resetRelevant = evReset != null && (isStringTables || isSyncTick);
                if (isSyncTick) {
                    ctl.setSyncTickSeen(true);
                }
                Event<OnMessage> ev = ctx.createEvent(OnMessage.class, messageClass);
                if (ev.isListenedTo() || resetRelevant) {
                    GeneratedMessage message = Packet.parse(messageClass, readPacket(src, size, isCompressed));
                    if (ev.isListenedTo()) {
                        ev.raise(message);
                    }
                    if (resetRelevant) {
                        ctl.markResetRelevantPacket(tick, kind, offset);
                        if (isStringTables) {
                            switch (loopCtl) {
                                case RESET_ACCUMULATE:
                                    evReset.raise(message, ResetPhase.ACCUMULATE);
                                    break;
                            }
                        }
                    }
                } else {
                    src.skipBytes(size);
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
            if (size < 0 || bs.remaining() < size * 8) {
                throw new IOException(
                        String.format("invalid embedded packet size: got %d remaining bits, but size is %d bits.", bs.remaining(), size * 8)
                );
            }
            Class<? extends GeneratedMessage> messageClass = ctx.getEngineType().embeddedPacketClassForKind(kind);
            if (messageClass == null) {
                logUnknownMessage(ctx, "embedded", kind);
                bs.skip(size * 8);
            } else {
                Event<OnMessage> ev = ctx.createEvent(OnMessage.class, messageClass);
                if (ev.isListenedTo() || (unpackUserMessages && messageClass == NetworkBaseTypes.CSVCMsg_UserMessage.class)) {
                    ensureBufferCapacity(2, size);
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
