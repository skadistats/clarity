package skadistats.clarity.io;

import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.model.state.StateMutation;

public class FieldChanges {

    private final FieldPath[] fieldPaths;
    private final StateMutation[] mutations;
    private final boolean capacityChanged;

    public FieldChanges(FieldPath[] source, int n, boolean capacityChanged) {
        this.fieldPaths = new FieldPath[n];
        System.arraycopy(source, 0, this.fieldPaths, 0, n);
        this.mutations = null;
        this.capacityChanged = capacityChanged;
    }

    public FieldChanges(FieldPath[] source, int n) {
        this.fieldPaths = new FieldPath[n];
        System.arraycopy(source, 0, this.fieldPaths, 0, n);
        this.mutations = new StateMutation[n];
        this.capacityChanged = false;
    }

    public boolean applyTo(EntityState state) {
        if (mutations == null) return capacityChanged;
        var result = false;
        for (var i = 0; i < fieldPaths.length; i++) {
            result |= state.applyMutation(fieldPaths[i], mutations[i]);
        }
        return result;
    }

    public void setMutation(int idx, StateMutation mutation) {
        mutations[idx] = mutation;
    }

    public FieldPath[] getFieldPaths() {
        return fieldPaths;
    }

    public StateMutation[] getMutations() {
        return mutations;
    }

    public boolean capacityChanged() {
        return capacityChanged;
    }

}
