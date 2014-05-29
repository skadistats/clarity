package skadistats.clarity.model;


public class GameEvent {

    private final GameEventDescriptor descriptor;
    private final Object[] state;

    public GameEvent(GameEventDescriptor descriptor) {
        this.descriptor = descriptor;
        this.state = new Object[descriptor.getKeys().length];
    }
    
    public void set(int index, Object value) {
        this.state[index] = value;
    }
    
    public <T> T getProperty(int index) {
        return (T) state[index];
    }

    public <T> T getProperty(String key) {
        Integer index = descriptor.getIndexForKey(key);
        if (index == null) {
            throw new IllegalArgumentException("key not found for this GameEvent");
        }
        return (T) state[index.intValue()];
    }

    public String getName() {
        return this.descriptor.getName();
    }
    
    public int getEventId() {
        return this.descriptor.getEventId();
    }
	
    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < state.length; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(descriptor.getKeys()[i]);
            buf.append("=");
            buf.append(state[i]);
        }
        return String.format("GameEvent [name=%s, %s]", descriptor.getName(), buf.toString());
    }
    
}
