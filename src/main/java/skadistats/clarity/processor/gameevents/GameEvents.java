package skadistats.clarity.processor.gameevents;

import skadistats.clarity.ClarityException;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.Initializer;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.GameEvent;
import skadistats.clarity.model.GameEventDescriptor;
import skadistats.clarity.processor.reader.OnMessage;
import skadistats.clarity.wire.common.proto.NetMessages;
import skadistats.clarity.wire.common.proto.NetworkBaseTypes;

import java.util.Map;
import java.util.TreeMap;

@Provides({ OnGameEventDescriptor.class, OnGameEvent.class})
public class GameEvents {

    private final Map<Integer, GameEventDescriptor> byId = new TreeMap<>();
    private final Map<String, GameEventDescriptor> byName = new TreeMap<>();

    @InsertEvent
    private Event<OnGameEventDescriptor> evGameEventDescriptor;
    @InsertEvent
    private Event<OnGameEvent> evGameEvent;

    @Initializer(OnGameEventDescriptor.class)
    public void initOnGameEventDescriptor(final EventListener<OnGameEventDescriptor> eventListener) {
        eventListener.setInvocationPredicate(args -> {
            String v = eventListener.getAnnotation().value();
            GameEventDescriptor ev = (GameEventDescriptor) args[0];
            return v.length() == 0 || v.equals(ev.getName());
        });
    }

    @Initializer(OnGameEvent.class)
    public void initOnGameEvent(final EventListener<OnGameEvent> eventListener) {
        eventListener.setInvocationPredicate(args -> {
            String v = eventListener.getAnnotation().value();
            GameEvent ev = (GameEvent) args[0];
            return v.length() == 0 || v.equals(ev.getName());
        });
    }

    @OnMessage(NetMessages.CSVCMsg_GameEventList.class)
    public void onGameEventList(NetMessages.CSVCMsg_GameEventList message) {
        for (NetMessages.CSVCMsg_GameEventList.descriptor_t d : message.getDescriptorsList()) {
            String[] keys = new String[d.getKeysCount()];
            for (int i = 0; i < d.getKeysCount(); i++) {
                NetMessages.CSVCMsg_GameEventList.key_t k = d.getKeys(i);
                keys[i] = k.getName();
            }
            GameEventDescriptor gev = new GameEventDescriptor(
                d.getEventid(),
                d.getName(),
                keys
            );
            byName.put(gev.getName(), gev);
            byId.put(gev.getEventId(), gev);
            evGameEventDescriptor.raise(gev);
        }
    }

    @OnMessage(NetworkBaseTypes.CSVCMsg_GameEvent.class)
    public void onGameEvent(NetworkBaseTypes.CSVCMsg_GameEvent message) {
        GameEventDescriptor desc = byId.get(message.getEventid());
        GameEvent e = new GameEvent(desc);
        for (int i = 0; i < message.getKeysCount(); i++) {
            NetworkBaseTypes.CSVCMsg_GameEvent.key_t key = message.getKeys(i);
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
                default:
                    throw new ClarityException("cannot handle game event key type %s", key.getType());
            }
            e.set(i, value);
        }
        evGameEvent.raise(e);
    }

}
