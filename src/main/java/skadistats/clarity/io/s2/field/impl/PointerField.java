package skadistats.clarity.io.s2.field.impl;

import skadistats.clarity.io.s2.Serializer;
import skadistats.clarity.io.s2.FieldType;
import skadistats.clarity.io.s2.field.DecoderProperties;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.model.state.ArrayEntityState;

public class PointerField extends RecordField {

    private final Decoder<?> baseDecoder;

    public PointerField(FieldType fieldType, DecoderProperties decoderProperties, Decoder<?> baseDecoder, Serializer serializer) {
        super(fieldType, decoderProperties, serializer);
        this.baseDecoder = baseDecoder;
    }

    @Override
    public Decoder<?> getDecoder() {
        return baseDecoder;
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
