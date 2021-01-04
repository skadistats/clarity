package skadistats.clarity.io.s2.field;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.s2.DecoderHolder;
import skadistats.clarity.io.s2.DecoderProperties;
import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.FieldType;
import skadistats.clarity.model.state.ArrayEntityState;

public class ValueField extends Field {

    private final DecoderHolder decoderHolder;

    public ValueField(FieldType fieldType, DecoderHolder decoderHolder) {
        super(fieldType);
        this.decoderHolder = decoderHolder;
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
    public void setArrayEntityState(ArrayEntityState state, int idx, Object value) {
        state.set(idx, value);
    }

    @Override
    public Object getArrayEntityState(ArrayEntityState state, int idx) {
        return state.get(idx);
    }

}
