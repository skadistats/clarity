package skadistats.clarity.decoder.s2.field.impl;

import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.s2.Field;
import skadistats.clarity.decoder.s2.field.FieldType;
import skadistats.clarity.decoder.s2.field.UnpackerProperties;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.state.ArrayEntityState;

public class ListField extends Field {

    private final Unpacker<?> lengthUnpacker;
    private final Field elementField;

    public ListField(FieldType fieldType, UnpackerProperties unpackerProperties, Unpacker<?> lengthUnpacker, Field elementField) {
        super(fieldType, unpackerProperties);
        this.lengthUnpacker = lengthUnpacker;
        this.elementField = elementField;
    }

    @Override
    public Unpacker<?> getUnpacker() {
        return lengthUnpacker;
    }

    @Override
    public String getChildNameSegment(int idx) {
        return Util.arrayIdxToString(idx);
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
        state.capacity(capacity, false);
    }

    @Override
    public void setArrayEntityState(ArrayEntityState state, int idx, Object value) {
        state.sub(idx).capacity((Integer) value, true);
    }

    @Override
    public Object getArrayEntityState(ArrayEntityState state, int idx) {
        return state.sub(idx).length();
    }

}
