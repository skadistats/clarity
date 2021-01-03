package skadistats.clarity.decoder.s2.field.impl;

import skadistats.clarity.decoder.s2.Field;
import skadistats.clarity.decoder.s2.Serializer;
import skadistats.clarity.decoder.s2.field.FieldProperties;
import skadistats.clarity.decoder.s2.field.UnpackerProperties;
import skadistats.clarity.model.state.ArrayEntityState;

public class RecordField extends Field {

    protected final Serializer serializer;

    public RecordField(FieldProperties fieldProperties, UnpackerProperties unpackerProperties, Serializer serializer) {
        super(fieldProperties, unpackerProperties);
        this.serializer = serializer;
    }

    public Serializer getSerializer() {
        return serializer;
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
