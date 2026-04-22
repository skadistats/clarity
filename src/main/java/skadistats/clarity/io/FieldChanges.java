package skadistats.clarity.io;

import skadistats.clarity.model.FieldPath;

import java.util.Arrays;

public class FieldChanges<FP extends FieldPath> {

    private final FP[] fieldPaths;
    private final boolean capacityChanged;

    public FieldChanges(FP[] source, int n, boolean capacityChanged) {
        this.fieldPaths = Arrays.copyOf(source, n);
        this.capacityChanged = capacityChanged;
    }

    public FP[] getFieldPaths() {
        return fieldPaths;
    }

    public boolean capacityChanged() {
        return capacityChanged;
    }

}
