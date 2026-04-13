package skadistats.clarity.io.s2.field;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.s2.FieldType;
import skadistats.clarity.io.s2.Pointer;
import skadistats.clarity.io.s2.Serializer;
import skadistats.clarity.io.s2.SerializerProperties;
import skadistats.clarity.model.state.StateMutation;

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
    public boolean isHiddenFieldPath() {
        return true;
    }

    @Override
    public SerializerProperties getSerializerProperties() {
        return serializerProperties;
    }

    @Override
    public Decoder getDecoder() {
        return decoder;
    }

    public void resetSerializer() {
        this.serializer = defaultSerializer;
    }

    public void activateSerializer(Serializer newSerializer) {
        this.serializer = newSerializer;
    }

    @Override
    public StateMutation createMutation(Object decodedValue, int depth) {
        var p = (Pointer) decodedValue;
        var typeIndex = p.getTypeIndex();
        var newSerializer = typeIndex != null ? serializers[typeIndex] : null;
        return new StateMutation.SwitchPointer(newSerializer);
    }

}
