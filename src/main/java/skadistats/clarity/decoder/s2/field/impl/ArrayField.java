package skadistats.clarity.decoder.s2.field.impl;

import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.s2.Field;
import skadistats.clarity.decoder.s2.field.FieldProperties;

public class ArrayField extends Field {

    private final Field elementField;

    public ArrayField(FieldProperties fieldProperties, Field elementField, int length) {
        super(fieldProperties, null);
        this.elementField = elementField;
    }

    @Override
    public Field getChild(int idx) {
        return elementField;
    }

    @Override
    public Integer getChildIndex(String name) {
        return Util.stringToArrayIdx(name);
    }

}
