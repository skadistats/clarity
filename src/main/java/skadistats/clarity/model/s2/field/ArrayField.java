package skadistats.clarity.model.s2.field;

import skadistats.clarity.io.Util;
import skadistats.clarity.model.s2.Field;
import skadistats.clarity.model.s2.FieldType;
import skadistats.clarity.state.s2.S2EntityState;

public class ArrayField extends Field {

    private final Field elementField;
    private final int length;

    public ArrayField(FieldType fieldType, Field elementField, int length) {
        super(fieldType);
        this.elementField = elementField;
        this.length = length;
    }

    public int getLength() {
        return length;
    }

    public Field getElementField() {
        return elementField;
    }

    @Override
    public Field getChild(S2EntityState state, int idx) {
        return elementField;
    }

    @Override
    public Integer getChildIndex(S2EntityState state, String nameSegment) {
        return Util.stringToArrayIdx(nameSegment);
    }

    @Override
    public String getChildNameSegment(S2EntityState state, int idx) {
        return Util.arrayIdxToString(idx);
    }

}
