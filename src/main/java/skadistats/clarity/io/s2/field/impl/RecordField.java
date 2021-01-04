package skadistats.clarity.io.s2.field.impl;

import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.Serializer;
import skadistats.clarity.io.s2.FieldType;
import skadistats.clarity.io.s2.DecoderProperties;
import skadistats.clarity.model.state.ArrayEntityState;

public class RecordField extends Field {

    protected final Serializer serializer;

    public RecordField(FieldType fieldType, DecoderProperties decoderProperties, Serializer serializer) {
        super(fieldType, decoderProperties);
        this.serializer = serializer;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    @Override
    public String getChildNameSegment(int idx) {
        return serializer.getFieldName(idx);
    }

    @Override
    public Field getChild(int idx) {
        return serializer.getField(idx);
    }

    @Override
    public Integer getChildIndex(String name) {
        return serializer.getFieldIndex(name);
    }

    @Override
    public void ensureArrayEntityStateCapacity(ArrayEntityState state, int capacity) {
        state.capacity(serializer.getFieldCount());
    }

}
