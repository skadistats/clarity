package skadistats.clarity.model.state;

import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s1.S1FieldPath;
import skadistats.clarity.util.SimpleIterator;

import java.util.Iterator;

public class ObjectArrayEntityState implements EntityState {

    private final Object[] state;

    public ObjectArrayEntityState(int length) {
        state = new Object[length];
    }

    private ObjectArrayEntityState(ObjectArrayEntityState other) {
        state = new Object[other.state.length];
        System.arraycopy(other.state, 0, state, 0, state.length);
    }

    @Override
    public EntityState copy() {
        return new ObjectArrayEntityState(this);
    }

    @Override
    public void setValueForFieldPath(FieldPath fp, Object value) {
        state[fp.s1().idx()] = value;
    }

    @Override
    public <T> T getValueForFieldPath(FieldPath fp) {
        return (T) state[fp.s1().idx()];
    }

    @Override
    public Iterator<FieldPath> fieldPathIterator() {
        return new SimpleIterator<FieldPath>() {
            int i = 0;
            @Override
            public FieldPath readNext() {
                return i < state.length ? new S1FieldPath(i++) : null;
            }
        };
    }

}
