package skadistats.clarity.io.s2.field;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.s2.FieldType;
import skadistats.clarity.io.s2.Pointer;
import skadistats.clarity.io.s2.Serializer;
import skadistats.clarity.io.s2.SerializerProperties;
import skadistats.clarity.model.state.ArrayEntityState;

public class PointerField extends SerializerField {

    private final Decoder decoder;
    private final SerializerProperties serializerProperties;
    private final Serializer[] serializers;
    private final Serializer defaultSerializer;

    public PointerField(FieldType fieldType, Decoder decoder, SerializerProperties serializerProperties, Serializer[] serializers) {
        super(fieldType, null);
        this.decoder = decoder;
        this.serializerProperties = serializerProperties;
        this.serializers = serializers;
        this.defaultSerializer = serializers.length == 1 ? serializers[0] : null;
        this.serializer = defaultSerializer;
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
    public boolean isHiddenFieldPath() {
        return true;
    }

    @Override
    public void setArrayEntityState(ArrayEntityState state, int idx, int childDepth, Object value) {
        Pointer p = (Pointer) value;
        var typeIndex = p.getTypeIndex();
        var newSerializer = typeIndex != null ? serializers[typeIndex] : null;
        if (state.has(idx)) {
            if (typeIndex == null || this.serializer != newSerializer) {
                this.serializer = defaultSerializer;
                state.clear(idx);
            }
        }
        if (newSerializer != null) {
            if (!state.has(idx)) {
                this.serializer = newSerializer;
                state.sub(idx);
            }
        }
    }

    @Override
    public Object getArrayEntityState(ArrayEntityState state, int idx) {
        return state.isSub(idx);
    }

}
