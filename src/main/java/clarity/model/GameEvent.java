package clarity.model;


public class GameEvent {

    private final GameEventDescriptor descriptor;
    private final Object[] state;

    public GameEvent(GameEventDescriptor descriptor) {
        this.descriptor = descriptor;
        this.state = new Object[descriptor.getKeyCount()];
    }
    
    public void set(int index, Object value) {
        this.state[index] = value;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < state.length; i++) {
            if (i > 0) {
                buf.append(' ');
            }
            buf.append(descriptor.getKeys(i).getName());
            buf.append("=");
            buf.append(state[i]);
        }
        return String.format("GameEvent [name=%s, state=[%s]]", descriptor.getName(), buf.toString());
    }
    
}
