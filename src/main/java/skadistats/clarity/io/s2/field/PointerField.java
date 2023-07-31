package skadistats.clarity.io.s2.field;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.s2.DecoderHolder;
import skadistats.clarity.io.s2.DecoderProperties;
import skadistats.clarity.io.s2.FieldType;
import skadistats.clarity.io.s2.S2DecoderFactory;
import skadistats.clarity.io.s2.Serializer;
import skadistats.clarity.model.state.ArrayEntityState;

public class PointerField extends SerializerField {

    private final DecoderHolder decoderHolder;

    public PointerField(FieldType fieldType, Serializer serializer) {
        super(fieldType, serializer);
        if ("CCSGameModeRules".equals(serializer.getId().getName())) {
            decoderHolder = S2DecoderFactory.createDecoder("gamerulesmode_fix");
        } else {
            decoderHolder = S2DecoderFactory.createDecoder("bool");
        }
    }

    @Override
    public DecoderProperties getDecoderProperties() {
        return decoderHolder.getDecoderProperties();
    }

    @Override
    public Decoder<?> getDecoder() {
        return decoderHolder.getDecoder();
    }

    @Override
    public boolean isHiddenFieldPath() {
        return true;
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
