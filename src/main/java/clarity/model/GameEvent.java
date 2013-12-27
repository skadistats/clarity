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
    
}
