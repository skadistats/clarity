package clarity.model;


public class GameEventDescriptor {

    private final int eventId;
    private final String name;
    private final String[] keys;
    
    public GameEventDescriptor(int eventId, String name, String[] keys) {
        this.eventId = eventId;
        this.name = name;
        this.keys = keys;
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
    
}
