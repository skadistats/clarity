package skadistats.clarity.model.state;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import skadistats.clarity.model.s2.S2FieldPath;

import java.util.Collection;

public class TreeMapEntityState implements EntityState<S2FieldPath> {

    private final Object2ObjectAVLTreeMap<S2FieldPath, Object> state;

    public TreeMapEntityState() {
        state = new Object2ObjectAVLTreeMap<>();
    }

    private TreeMapEntityState(TreeMapEntityState other) {
        state = other.state.clone();
    }

    @Override
    public EntityState clone() {
        return new TreeMapEntityState(this);
    }

    @Override
    public void setValueForFieldPath(S2FieldPath fp, Object value) {
        state.put(fp, value);
    }

    @Override
    public <T> T getValueForFieldPath(S2FieldPath fp) {
        return (T) state.get(fp);
    }

    @Override
    public Collection<S2FieldPath> collectFieldPaths() {
        return state.keySet();
    }

}
