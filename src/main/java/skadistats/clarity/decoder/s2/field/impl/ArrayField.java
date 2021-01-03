package skadistats.clarity.decoder.s2.field.impl;

import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.s2.Field;
import skadistats.clarity.decoder.s2.field.FieldProperties;
import skadistats.clarity.model.state.ArrayEntityState;

public class ArrayField extends Field {

    private final Field elementField;
    private final int length;

    public ArrayField(FieldProperties fieldProperties, Field elementField, int length) {
        super(fieldProperties, null);
        this.elementField = elementField;
        this.length = length;
    }

    @Override
    public Field getChild(int idx) {
        return elementField;
    }

    @Override
    public Integer getChildIndex(String name) {
        return Util.stringToArrayIdx(name);
    }

    @Override
    public void ensureArrayEntityStateCapacity(ArrayEntityState state, int capacity) {
        state.capacity(length);
    }

    @Override
    public Object getArrayEntityState(ArrayEntityState state, int idx) {
        return state.sub(idx).length();
    }

}
