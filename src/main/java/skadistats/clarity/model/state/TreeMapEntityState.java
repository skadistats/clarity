package skadistats.clarity.model.state;

import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import skadistats.clarity.model.FieldPath;

import java.util.Collection;

public class TreeMapEntityState implements EntityState {

    private final Object2ObjectAVLTreeMap<FieldPath, Object> state;

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
    public void setValueForFieldPath(FieldPath fp, Object value) {
        state.put(fp, value);
    }

    @Override
    public <T> T getValueForFieldPath(FieldPath fp) {
        return (T) state.get(fp);
    }

    @Override
    public Collection<FieldPath> collectFieldPaths() {
        return state.keySet();
    }

}
