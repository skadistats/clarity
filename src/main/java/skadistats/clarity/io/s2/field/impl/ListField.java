package skadistats.clarity.io.s2.field.impl;

import skadistats.clarity.io.Util;
import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.field.FieldType;
import skadistats.clarity.io.s2.field.DecoderProperties;
import skadistats.clarity.io.unpacker.Decoder;
import skadistats.clarity.model.state.ArrayEntityState;

public class ListField extends Field {

    private final Decoder<?> lengthDecoder;
    private final Field elementField;

    public ListField(FieldType fieldType, DecoderProperties decoderProperties, Decoder<?> lengthDecoder, Field elementField) {
        super(fieldType, decoderProperties);
        this.lengthDecoder = lengthDecoder;
        this.elementField = elementField;
    }

    @Override
    public Decoder<?> getDecoder() {
        return lengthDecoder;
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
