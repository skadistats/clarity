package skadistats.clarity.io;

import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.model.state.StateMutation;

import java.util.Arrays;
import java.util.function.BiConsumer;

public class FieldChanges<FP extends FieldPath> {

    private final FP[] fieldPaths;
    private final StateMutation[] mutations;
    private final boolean capacityChanged;

    public FieldChanges(FP[] source, int n, boolean capacityChanged) {
        this.fieldPaths = Arrays.copyOf(source, n);
        this.mutations = null;
        this.capacityChanged = capacityChanged;
    }

    public FieldChanges(FP[] source, int n) {
        this.fieldPaths = Arrays.copyOf(source, n);
        this.mutations = new StateMutation[n];
        this.capacityChanged = false;
    }

    public FP[] getFieldPaths() {
        return fieldPaths;
    }

    public StateMutation[] getMutations() {
        return mutations;
    }

    public boolean capacityChanged() {
        return capacityChanged;
    }

    public boolean applyTo(EntityState state) {
        return applyTo(state, null);
    }

    public boolean applyTo(EntityState state, BiConsumer<FieldPath, StateMutation> beforeEach) {
        return mutations == null ? capacityChanged : EntityState.applyMutations(state, fieldPaths, mutations, beforeEach);
    }

    public void setMutation(int idx, StateMutation mutation) {
        mutations[idx] = mutation;
    }

}
