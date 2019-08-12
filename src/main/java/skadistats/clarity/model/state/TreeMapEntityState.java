package skadistats.clarity.model.state;

import skadistats.clarity.model.FieldPath;

import java.util.Collection;
import java.util.TreeMap;

public class TreeMapEntityState implements EntityState {

    private final TreeMap<FieldPath, Object> state = new TreeMap<>();

    @Override
    public EntityState clone() {
        final TreeMapEntityState clone = new TreeMapEntityState();
        clone.state.putAll(state);
        return clone;
    }

    @Override
    public void setValueForFieldPath(FieldPath fp, Object value) {
        state.put(fp, value);
    }

    @Override
    public <T> T getValueForFieldPath(FieldPath fp) {
        return (T) state.get(fp);
    }

    @Override
    public String getNameForFieldPath(FieldPath fp) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<FieldPath> collectFieldPaths() {
        return state.keySet();
    }

}
