package skadistats.clarity.decoder.s2.field.impl;

import skadistats.clarity.decoder.s2.Field;
import skadistats.clarity.decoder.s2.field.FieldProperties;
import skadistats.clarity.decoder.s2.field.UnpackerProperties;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.state.ArrayEntityState;

public class ValueField extends Field {

    private final Unpacker<?> unpacker;

    public ValueField(FieldProperties fieldProperties, UnpackerProperties unpackerProperties, Unpacker<?> unpacker) {
        super(fieldProperties, unpackerProperties);
        this.unpacker = unpacker;
    }

    @Override
    public Unpacker<?> getUnpacker() {
        return unpacker;
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
