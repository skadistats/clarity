package skadistats.clarity.model;

import java.util.HashMap;
import java.util.Map;


public class GameEventDescriptor {

    private final int eventId;
    private final String name;
    private final String[] keys;
    private Map<String, Integer> indexByKey = new HashMap<String, Integer>();
    
    
    public GameEventDescriptor(int eventId, String name, String[] keys) {
        this.eventId = eventId;
        this.name = name;
        this.keys = keys;
        for (int i = 0; i < keys.length; i++) {
            indexByKey.put(keys[i], i);
        }
    }

    public int getEventId() {
        return eventId;
    }

    public String getName() {
        return name;
    }

    public String[] getKeys() {
        return keys;
    }
    
    public Integer getIndexForKey(String key) {
        return indexByKey.get(key);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("GameEventDescriptor [");
        sb.append("eventId=").append(eventId);
        sb.append(", name='").append(name).append('\'');
        sb.append(']');
        return sb.toString();
    }

}
