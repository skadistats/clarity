package skadistats.clarity.model;

import java.util.HashMap;
import java.util.Map;


/**
 * Describes a game event type: id, name and the ordered list of its keys.
 */
public class GameEventDescriptor {

    private final int eventId;
    private final String name;
    private final String[] keys;
    private final Map<String, Integer> indexByKey = new HashMap<>();


    /** Created by the parser from the game event list. */
    public GameEventDescriptor(int eventId, String name, String[] keys) {
        this.eventId = eventId;
        this.name = name;
        this.keys = keys;
        for (var i = 0; i < keys.length; i++) {
            indexByKey.put(keys[i], i);
        }
    }

    /** @return the numeric id game events of this type are sent with */
    public int getEventId() {
        return eventId;
    }

    /** @return the event name */
    public String getName() {
        return name;
    }

    /**
     * @return the key names in value order; the backing array, do not modify
     */
    public String[] getKeys() {
        return keys;
    }

    /**
     * @return the position of {@code key} in {@link #getKeys()}, or {@code null} if there is no such key
     */
    public Integer getIndexForKey(String key) {
        return indexByKey.get(key);
    }

    @Override
    public String toString() {
        final var sb = new StringBuilder("GameEventDescriptor [");
        sb.append("eventId=").append(eventId);
        sb.append(", name='").append(name).append('\'');
        sb.append(']');
        return sb.toString();
    }

}
