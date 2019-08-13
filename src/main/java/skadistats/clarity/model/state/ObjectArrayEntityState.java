package skadistats.clarity.model.state;

import skadistats.clarity.model.s1.S1FieldPath;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ObjectArrayEntityState implements EntityState<S1FieldPath> {

    private final Object[] state;

    public ObjectArrayEntityState(int length) {
        state = new Object[length];
    }

    private ObjectArrayEntityState(ObjectArrayEntityState other) {
        state = new Object[other.state.length];
        System.arraycopy(other.state, 0, state, 0, state.length);
    }

    @Override
    public EntityState clone() {
        return new ObjectArrayEntityState(this);
    }

    @Override
    public void setValueForFieldPath(S1FieldPath fp, Object value) {
        state[fp.cur()] = value;
    }

    @Override
    public <T> T getValueForFieldPath(S1FieldPath fp) {
        return (T) state[fp.cur()];
    }

    @Override
    public Collection<S1FieldPath> collectFieldPaths() {
        List<S1FieldPath> result = new ArrayList<>(state.length);
        for (int i = 0; i < state.length; i++) {
            result.add(new S1FieldPath(i));
        }
        return result;
    }

}
