package skadistats.clarity.io.s2.field;

import skadistats.clarity.io.Util;
import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.FieldType;
import skadistats.clarity.model.state.NestedEntityState;

public class ArrayField extends Field {

    private final Field elementField;
    private final int length;

    public ArrayField(FieldType fieldType, Field elementField, int length) {
        super(fieldType);
        this.elementField = elementField;
        this.length = length;
    }

    @Override
    public Field getChild(int idx) {
        return elementField;
    }

    @Override
    public Integer getChildIndex(String nameSegment) {
        return Util.stringToArrayIdx(nameSegment);
    }

    @Override
    public String getChildNameSegment(int idx) {
        return Util.arrayIdxToString(idx);
    }

    @Override
    public void ensureCapacity(NestedEntityState state, int capacity) {
        state.capacity(length, false);
    }

    @Override
    public Object getValue(NestedEntityState state, int idx) {
        return state.sub(idx).length();
    }

}
