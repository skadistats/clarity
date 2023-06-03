package skadistats.clarity.processor.gameevents;

import org.slf4j.Logger;
import skadistats.clarity.ClarityException;
import skadistats.clarity.LogChannel;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.Initializer;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.logger.PrintfLoggerFactory;
import skadistats.clarity.model.GameEvent;
import skadistats.clarity.model.GameEventDescriptor;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.wire.shared.common.proto.CommonNetMessages;
import skadistats.clarity.wire.shared.common.proto.CommonNetworkBaseTypes;

import java.util.ArrayList;
import java.util.List;

@Provides({ OnGameEventDescriptor.class, OnGameEvent.class})
public class GameEvents {

    private static final Logger log = PrintfLoggerFactory.getLogger(LogChannel.runner);

    private GameEventDescriptor[] descriptors;
    private List<CommonNetworkBaseTypes.CSVCMsg_GameEvent> preListBuffer;

    @InsertEvent
    private Event<OnGameEventDescriptor> evGameEventDescriptor;
    @InsertEvent
    private Event<OnGameEvent> evGameEvent;

    @Initializer(OnGameEventDescriptor.class)
    public void initOnGameEventDescriptor(final EventListener<OnGameEventDescriptor> eventListener) {
        eventListener.setInvocationPredicate(args -> {
            var v = eventListener.getAnnotation().value();
            var ev = (GameEventDescriptor) args[0];
            return v.length() == 0 || v.equals(ev.getName());
        });
    }

    @Initializer(OnGameEvent.class)
    public void initOnGameEvent(final EventListener<OnGameEvent> eventListener) {
        eventListener.setInvocationPredicate(args -> {
            var v = eventListener.getAnnotation().value();
            var ev = (GameEvent) args[0];
            return v.length() == 0 || v.equals(ev.getName());
        });
    }

    @OnMessage(CommonNetMessages.CSVCMsg_GameEventList.class)
    public void onGameEventList(CommonNetMessages.CSVCMsg_GameEventList message) {
        var descriptorMax = message.getDescriptors(message.getDescriptorsCount() - 1).getEventid();
        descriptors = new GameEventDescriptor[descriptorMax + 1];
        for (var d : message.getDescriptorsList()) {
            var i = d.getEventid();

            var keyCount = d.getKeysCount();
            var keys = new String[keyCount];
            for (var k = 0; k < keyCount; k++) {
                var key = d.getKeys(k);
                keys[k] = key.getName();
            }

            var gev = new GameEventDescriptor(
                i,
                d.getName(),
                keys
            );

            descriptors[i] = gev;
            evGameEventDescriptor.raise(gev);
        }
        if (preListBuffer != null) {
            preListBuffer.forEach(this::onGameEvent);
            preListBuffer = null;
        }
    }

    @OnMessage(CommonNetworkBaseTypes.CSVCMsg_GameEvent.class)
    public void onGameEvent(CommonNetworkBaseTypes.CSVCMsg_GameEvent message) {
        if (descriptors == null) {
            if (preListBuffer == null) {
                preListBuffer = new ArrayList<>();
            }
            preListBuffer.add(message);
            return;
        }
        var descriptor = descriptors[message.getEventid()];
        if (descriptor == null) {
            log.warn("received game event with unknown event-id %d", message.getEventid());
            return;
        }
        var e = new GameEvent(descriptor);
        for (var i = 0; i < message.getKeysCount(); i++) {
            var key = message.getKeys(i);
            Object value;
            switch (key.getType()) {
                case 1:
                    value = key.getValString();
                    break;
                case 2:
                    value = key.getValFloat();
                    break;
                case 3:
                    value = key.getValLong();
                    break;
                case 4:
                    value = key.getValShort();
                    break;
                case 5:
                    value = key.getValByte();
                    break;
                case 6:
                    value = key.getValBool();
                    break;
                case 7:
                    value = key.getValUint64();
                    break;
                case 8:
                    // TODO: this is something special encoded as a long. Find out what it is.
                    value = key.getValLong();
                    break;
                case 9:
                    // TODO: this is something special encoded as a short. Find out what it is.
                    value = key.getValShort();
                    break;

                // TODO: protobuf has a type wstring
                // TODO: this is probably how to interpret it
                // new String(ZeroCopy.extract(key.getValWstring()), StandardCharsets.UTF_16LE);

                default:
                    throw new ClarityException("cannot handle game event key type %s", key.getType());
            }
            e.set(i, value);
        }
        evGameEvent.raise(e);
    }

}
