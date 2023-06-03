package skadistats.clarity.io;

import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.EntityState;

public class FieldChanges {

    private final FieldPath[] fieldPaths;
    private final Object[] values;

    public FieldChanges(FieldPath[] source, int n) {
        this.fieldPaths = new FieldPath[n];
        System.arraycopy(source, 0, this.fieldPaths, 0, n);
        this.values = new Object[n];
    }

    public boolean applyTo(EntityState state) {
        var capacityChanged = false;
        for (var i = 0; i < fieldPaths.length; i++) {
            capacityChanged |= state.setValueForFieldPath(fieldPaths[i], values[i]);
        }
        return capacityChanged;
    }

    public Object getValue(int idx) {
        return values[idx];
    }

    public void setValue(int idx, Object value) {
        values[idx] = value;
    }

    public FieldPath[] getFieldPaths() {
        return fieldPaths;
    }

}
