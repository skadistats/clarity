package skadistats.clarity.io.s2.field;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.FieldType;
import skadistats.clarity.io.s2.SerializerProperties;
import skadistats.clarity.model.state.ArrayEntityState;

public class ValueField extends Field {

    private final Decoder decoder;
    private final SerializerProperties serializerProperties;

    public ValueField(FieldType fieldType, Decoder decoder, SerializerProperties serializerProperties) {
        super(fieldType);
        this.decoder = decoder;
        this.serializerProperties = serializerProperties;
    }

    @Override
    public SerializerProperties getSerializerProperties() {
        return serializerProperties;
    }

    @Override
    public Decoder getDecoder() {
        return decoder;
    }

    @Override
    public void setArrayEntityState(ArrayEntityState state, int idx, int childDepth, Object value) {
        state.set(idx, value);
    }

    @Override
    public Object getArrayEntityState(ArrayEntityState state, int idx) {
        return state.get(idx);
    }

}
