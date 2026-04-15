package skadistats.clarity.io;

import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.model.state.StateMutation;

public class FieldChanges {

    private final FieldPath[] fieldPaths;
    private final StateMutation[] mutations;

    public FieldChanges(FieldPath[] source, int n) {
        this.fieldPaths = new FieldPath[n];
        System.arraycopy(source, 0, this.fieldPaths, 0, n);
        this.mutations = new StateMutation[n];
    }

    public boolean applyTo(EntityState state) {
        var capacityChanged = false;
        for (var i = 0; i < fieldPaths.length; i++) {
            capacityChanged |= state.applyMutation(fieldPaths[i], mutations[i]);
        }
        return capacityChanged;
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

}
