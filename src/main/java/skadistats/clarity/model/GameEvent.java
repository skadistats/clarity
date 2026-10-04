package skadistats.clarity.model;


/**
 * One occurrence of a game event: a {@link GameEventDescriptor} plus the
 * values for its keys. Values are stored as objects and read by key name or
 * key index.
 */
public class GameEvent {

    private final GameEventDescriptor descriptor;
    private final Object[] state;

    /** Created by the parser. */
    public GameEvent(GameEventDescriptor descriptor) {
        this.descriptor = descriptor;
        this.state = new Object[descriptor.getKeys().length];
    }
    
    /** Internal to the parser. */
    public void set(int index, Object value) {
        this.state[index] = value;
    }
    
    /**
     * Reads the value at key position {@code index}. The value is cast
     * unchecked to {@code T}.
     */
    public <T> T getProperty(int index) {
        return (T) state[index];
    }

    /**
     * Reads the value for key {@code property}. The value is cast unchecked to {@code T}.
     *
     * @throws IllegalArgumentException if the event has no such key
     */
    public <T> T getProperty(String property) {
        var index = descriptor.getIndexForKey(property);
        if (index == null) {
            throw new IllegalArgumentException(String.format("property %s not found on game event of class %s", property, descriptor.getName()));
        }
        return (T) state[index.intValue()];
    }

    /**
     * @return the event name
     */
    public String getName() {
        return this.descriptor.getName();
    }
    
    /**
     * @return the event id
     */
    public int getEventId() {
        return this.descriptor.getEventId();
    }
	
    @Override
    public String toString() {
        var buf = new StringBuilder();
        for (var i = 0; i < state.length; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(descriptor.getKeys()[i]);
            buf.append("=");
            buf.append(state[i]);
        }
        return String.format("GameEvent [name=%s, id=%s%s%s]", descriptor.getName(), descriptor.getEventId(), buf.length() > 0 ? ", " : "", buf.toString());
    }
    
}
