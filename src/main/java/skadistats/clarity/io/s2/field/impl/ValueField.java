package skadistats.clarity.io.s2.field.impl;

import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.field.FieldType;
import skadistats.clarity.io.s2.field.DecoderProperties;
import skadistats.clarity.io.unpacker.Decoder;
import skadistats.clarity.model.state.ArrayEntityState;

public class ValueField extends Field {

    private final Decoder<?> decoder;

    public ValueField(FieldType fieldType, DecoderProperties decoderProperties, Decoder<?> decoder) {
        super(fieldType, decoderProperties);
        this.decoder = decoder;
    }

    @Override
    public Decoder<?> getDecoder() {
        return decoder;
    }

    @Override
    public void setArrayEntityState(ArrayEntityState state, int idx, Object value) {
        state.set(idx, value);
    }

    @Override
    public Object getArrayEntityState(ArrayEntityState state, int idx) {
        return state.get(idx);
    }

}
