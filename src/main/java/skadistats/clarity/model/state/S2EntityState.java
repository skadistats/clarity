package skadistats.clarity.model.state;

import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.field.SerializerField;
import skadistats.clarity.model.FieldPath;

public abstract class S2EntityState implements EntityState, NestedEntityState {

    protected final SerializerField rootField;
    protected boolean capacityChanged;

    protected S2EntityState(SerializerField rootField) {
        this.rootField = rootField;
    }

    @Override
    public boolean setValueForFieldPath(FieldPath fpX, Object value) {
        var fp = fpX.s2();

        Field field = rootField;
        NestedEntityState node = this;
        var last = fp.last();

        capacityChanged = false;
        var i = 0;
        while (true) {
            var idx = fp.get(i);
            if (node.length() <= idx) {
                field.ensureCapacity(node, idx + 1);
            }
            field = field.getChild(idx);
            if (i == last) {
                field.setValue(node, idx, i + 1, value);
                return capacityChanged;
            }
            node = node.sub(idx);
            i++;
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getValueForFieldPath(FieldPath fpX) {
        var fp = fpX.s2();

        Field field = rootField;
        NestedEntityState node = this;
        var last = fp.last();

        var i = 0;
        while (true) {
            var idx = fp.get(i);
            field = field.getChild(idx);
            if (i == last) {
                return (T) field.getValue(node, idx);
            }
            if (!node.isSub(idx)) {
                return null;
            }
            node = node.sub(idx);
            i++;
        }
    }

}
