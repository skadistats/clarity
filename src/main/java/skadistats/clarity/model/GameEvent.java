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

    public <T> T getProperty(String property) {
        Integer index = descriptor.getIndexForKey(property);
        if (index == null) {
            throw new IllegalArgumentException(String.format("property %s not found on game event of class %s", property, descriptor.getName()));
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
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < state.length; i++) {
            if (i > 0) {
                buf.append(", ");
            }
            buf.append(descriptor.getKeys()[i]);
            buf.append("=");
            buf.append(state[i]);
        }
        return String.format("GameEvent [name=%s, id=%s, %s]", descriptor.getName(), descriptor.getEventId(), buf.toString());
    }
    
}
