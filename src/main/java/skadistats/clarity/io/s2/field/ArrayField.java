package skadistats.clarity.io.s2.field;

import skadistats.clarity.io.Util;
import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.FieldType;

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

}
