package skadistats.clarity.processor.reader;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;
import com.google.protobuf.ZeroCopy;
import org.slf4j.Logger;
import skadistats.clarity.LogChannel;
import skadistats.clarity.engine.EngineType;
import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.Initializer;
import skadistats.clarity.event.Insert;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.logger.PrintfLoggerFactory;
import skadistats.clarity.processor.packet.PacketReader;
import skadistats.clarity.processor.packet.UsesPacketReader;
import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.processor.runner.FileRunner;
import skadistats.clarity.processor.runner.LoopController;
import skadistats.clarity.processor.runner.OnInputSource;
import skadistats.clarity.source.Source;
import skadistats.clarity.wire.Packet;
import skadistats.clarity.wire.shared.common.proto.CommonNetMessages;
import skadistats.clarity.wire.shared.demo.proto.Demo;

import java.io.EOFException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Provides(value = {OnMessageContainer.class, OnMessage.class, OnPostEmbeddedMessage.class, OnReset.class, OnFullPacket.class}, runnerClass = {FileRunner.class})
@UsesPacketReader
public class InputSourceProcessor {

    private static final Logger log = PrintfLoggerFactory.getLogger(LogChannel.runner);

    private boolean unpackUserMessages = false;

    @Insert
    private Context ctx;
    @Insert
    private EngineType engineType;
    @Insert
    private PacketReader packetReader;

    @InsertEvent
    private OnReset.Event evReset;
    @InsertEvent
    private OnFullPacket.Event evFull;
    @InsertEvent
    private OnMessageContainer.Event evMessageContainer;
    @InsertEvent
    private OnMessage.Event evOnMessage;
    @InsertEvent
    private OnPostEmbeddedMessage.Event evOnPostEmbeddedMessage;

    private final Set<Integer> alreadyLoggedUnknowns = new HashSet<>();

    @Initializer(OnMessage.class)
    public void initOnMessageListener(final EventListener<OnMessage> listener) {
        var messageClass = listener.getAnnotation().value();
        unpackUserMessages |= messageClass == GeneratedMessage.class || engineType.isUserMessage(messageClass);
    }

    @Initializer(OnPostEmbeddedMessage.class)
    public void initOnPostEmbeddedMessageListener(final EventListener<OnPostEmbeddedMessage> listener) {
        var messageClass = listener.getAnnotation().value();
        unpackUserMessages |= messageClass == GeneratedMessage.class || engineType.isUserMessage(messageClass);
    }

    @Initializer(OnMessageContainer.class)
    public void initOnMessageContainerListener(final EventListener<OnMessageContainer> listener) {
        var containerClass = listener.getAnnotation().value();
        if (containerClass != GeneratedMessage.class) {
            listener.setFilter((OnMessageContainer.Filter) (clazz, bytes) -> containerClass.isAssignableFrom(clazz));
        }
    }

    private void logUnknownMessage(String where, int type) {
        var hash = (where.hashCode() * 31 + engineType.hashCode()) * 31 + type;
        if (!alreadyLoggedUnknowns.contains(hash)) {
            alreadyLoggedUnknowns.add(hash);
            log.warn("unknown %s message of kind %s/%d. Please report this in the corresponding issue: https://github.com/skadistats/clarity/issues/58", where, engineType, type);
        }
    }

    @OnInputSource
    public void processSource(Source src, LoopController ctl) throws Exception {

        ByteString resetFullPacketData = null;

        int offset;

        PacketInstance<?> pi;
        LoopController.Command loopCtl;

        main:
        while (true) {
            offset = src.getPosition();
            try {
                pi = engineType.getNextPacketInstance(src);
            } catch (EOFException e) {
                pi = PacketInstance.EOF;
            }
            loopctl:
            while (true) {
                loopCtl = ctl.doLoopControl(pi.getTick());
                switch (loopCtl) {
                    case RESET_START:
                        evReset.raise(null, ResetPhase.START);
                        continue loopctl;
                    case RESET_CLEAR:
                        evReset.raise(null, ResetPhase.CLEAR);
                        continue loopctl;
                    case RESET_ACCUMULATE:
                        // accumulate a single string table change
                        break loopctl;
                    case RESET_APPLY:
                        // apply string table changes
                        evReset.raise(null, ResetPhase.APPLY);
                        if (resetFullPacketData != null) {
                            // apply full packet for entities
                            evMessageContainer.raise(Demo.CDemoFullPacket.class, resetFullPacketData);
                            resetFullPacketData = null;
                        }
                        continue loopctl;
                    case RESET_COMPLETE:
                        evReset.raise(null, ResetPhase.COMPLETE);
                        continue loopctl;
                    case FALLTHROUGH:
                        if (pi.getTick() != Integer.MAX_VALUE) {
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

            var messageClass = pi.getMessageClass();
            if (messageClass == null) {
                logUnknownMessage("top level", pi.getKind());
                pi.skip();
            } else if (messageClass == Demo.CDemoPacket.class) {
                var message = (Demo.CDemoPacket) pi.parse();
                evMessageContainer.raise(Demo.CDemoPacket.class, message.getData());
            } else if (engineType.isSendTablesContainer() && messageClass == Demo.CDemoSendTables.class) {
                var message = (Demo.CDemoSendTables) pi.parse();
                evMessageContainer.raise(Demo.CDemoSendTables.class, message.getData());
            } else if (messageClass == Demo.CDemoFullPacket.class) {
                if (evFull.isListenedTo() || evReset.isListenedTo()) {
                    var message = (Demo.CDemoFullPacket) pi.parse();
                    evFull.raise(message);
                    if (evReset.isListenedTo()) {
                        ctl.markResetRelevantPacket(pi.getTick(), pi.getResetRelevantKind(), offset);
                        switch (loopCtl) {
                            case RESET_ACCUMULATE:
                                evReset.raise(message.getStringTable(), ResetPhase.ACCUMULATE);
                                resetFullPacketData = message.getPacket().getData();
                                break;
                        }
                    }
                } else {
                    pi.skip();
                }
            } else {
                var isStringTables = messageClass == Demo.CDemoStringTables.class;
                var isSyncTick = messageClass == Demo.CDemoSyncTick.class;
                var resetRelevant = evReset != null && (isStringTables || isSyncTick);
                if (isSyncTick) {
                    ctl.setSyncTickSeen(true);
                }
                if (evOnMessage.isListenedTo(messageClass) || resetRelevant) {
                    var message = pi.parse();
                    evOnMessage.raise(message);
                    if (resetRelevant) {
                        ctl.markResetRelevantPacket(pi.getTick(), pi.getResetRelevantKind(), offset);
                        if (isStringTables) {
                            switch (loopCtl) {
                                case RESET_ACCUMULATE:
                                    evReset.raise((Demo.CDemoStringTables) message, ResetPhase.ACCUMULATE);
                                    break;
                            }
                        }
                    }
                } else {
                    pi.skip();
                }
            }
        }
    }

    @OnMessageContainer
    public void processEmbedded(Class<? extends GeneratedMessage> containerClass, ByteString bytes) throws IOException {
        var bs = BitStream.createBitStream(bytes);
        while (bs.remaining() >= 8) {
            var kind = engineType.readEmbeddedKind(bs);
            if (kind == 0) {
                // this seems to happen with console recorded replays
                break;
            }
            var size = bs.readVarUInt();
            if (size < 0 || bs.remaining() < size * 8) {
                throw new IOException(
                        String.format("invalid embedded packet size: got %d remaining bits, but size is %d bits.", bs.remaining(), size * 8)
                );
            }
            var messageClass = engineType.embeddedPacketClassForKind(kind);
            if (messageClass == null) {
                logUnknownMessage("embedded", kind);
                bs.skip(size * 8);
            } else {
                if (evOnMessage.isListenedTo(messageClass) || evOnPostEmbeddedMessage.isListenedTo(messageClass) || (unpackUserMessages && messageClass == CommonNetMessages.CSVCMsg_UserMessage.class)) {
                    var subMessage = Packet.parse(messageClass, ZeroCopy.wrap(packetReader.readFromBitStream(bs, size * 8)));
                    if (evOnMessage.isListenedTo(messageClass)) {
                        evOnMessage.raise(subMessage);
                    }
                    if (unpackUserMessages && messageClass == CommonNetMessages.CSVCMsg_UserMessage.class) {
                        var userMessage = (CommonNetMessages.CSVCMsg_UserMessage) subMessage;
                        var umClazz = engineType.userMessagePacketClassForKind(userMessage.getMsgType());
                        if (umClazz == null) {
                            logUnknownMessage("usermessage", userMessage.getMsgType());
                        } else {
                            evOnMessage.raise(Packet.parse(umClazz, userMessage.getMsgData()));
                        }
                    }
                    if (evOnPostEmbeddedMessage.isListenedTo(messageClass)) {
                        evOnPostEmbeddedMessage.raise(subMessage, bs);
                    }
                } else {
                    bs.skip(size * 8);
                }
            }
        }
    }

}
