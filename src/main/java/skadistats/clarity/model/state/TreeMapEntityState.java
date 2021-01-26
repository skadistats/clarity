package skadistats.clarity.model.state;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import skadistats.clarity.model.FieldPath;

import java.util.Iterator;

public class TreeMapEntityState implements EntityState {

    private final Object2ObjectAVLTreeMap<FieldPath, Object> state;

    public TreeMapEntityState() {
        state = new Object2ObjectAVLTreeMap<>();
    }

    private TreeMapEntityState(TreeMapEntityState other) {
        state = other.state.clone();
    }

    @Override
    public EntityState copy() {
        return new TreeMapEntityState(this);
    }

    @Override
    public boolean setValueForFieldPath(FieldPath fp, Object value) {
        if (value != null) {
            return state.put(fp, value) == null;
        } else {
            return state.remove(fp) != null;
        }
    }

    @Override
    public <T> T getValueForFieldPath(FieldPath fp) {
        return (T) state.get(fp);
    }

    @Override
    public Iterator<FieldPath> fieldPathIterator() {
        return state.keySet().iterator();
    }

}
