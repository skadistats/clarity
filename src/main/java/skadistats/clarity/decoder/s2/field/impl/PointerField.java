package skadistats.clarity.decoder.s2.field.impl;

import skadistats.clarity.decoder.s2.Serializer;
import skadistats.clarity.decoder.s2.field.FieldType;
import skadistats.clarity.decoder.s2.field.UnpackerProperties;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.state.ArrayEntityState;

public class PointerField extends RecordField {

    private final Unpacker<?> baseUnpacker;

    public PointerField(FieldType fieldType, UnpackerProperties unpackerProperties, Unpacker<?> baseUnpacker, Serializer serializer) {
        super(fieldType, unpackerProperties, serializer);
        this.baseUnpacker = baseUnpacker;
    }

    @Override
    public Unpacker<?> getUnpacker() {
        return baseUnpacker;
    }

    @Override
    public void setArrayEntityState(ArrayEntityState state, int idx, Object value) {
        boolean existing = (Boolean) value;
        if (state.has(idx) ^ existing) {
            if (existing) {
                state.sub(idx);
            } else {
                state.clear(idx);
            }
        }
    }

    @Override
    public Object getArrayEntityState(ArrayEntityState state, int idx) {
        return state.isSub(idx);
    }

}
