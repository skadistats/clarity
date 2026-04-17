package skadistats.clarity.model.s2.field;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.model.s2.Field;
import skadistats.clarity.model.s2.FieldType;
import skadistats.clarity.model.s2.Pointer;
import skadistats.clarity.model.s2.Serializer;
import skadistats.clarity.model.s2.SerializerProperties;
import skadistats.clarity.state.StateMutation;
import skadistats.clarity.state.s2.S2EntityState;

public final class PointerField extends Field {

    private final Decoder decoder;
    private final SerializerProperties serializerProperties;
    private final Serializer[] serializers;
    private final Serializer defaultSerializer;
    private int pointerId;

    public PointerField(FieldType fieldType, Decoder decoder, SerializerProperties serializerProperties, Serializer[] serializers) {
        super(fieldType);
        this.decoder = decoder;
        this.serializerProperties = serializerProperties;
        this.serializers = serializers;
        this.defaultSerializer = serializers.length == 1 ? serializers[0] : null;
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

    public int getPointerId() {
        return pointerId;
    }

    public void setPointerId(int pointerId) {
        this.pointerId = pointerId;
    }

    public Serializer[] getSerializers() {
        return serializers;
    }

    private Serializer resolveSerializer(S2EntityState state) {
        var ser = state.getPointerSerializer(pointerId);
        return ser != null ? ser : defaultSerializer;
    }

    @Override
    public Field getChild(S2EntityState state, int idx) {
        var ser = resolveSerializer(state);
        return ser != null ? ser.getField(idx) : null;
    }

    public Field getChild(int idx, S2EntityState state, Serializer override) {
        var ser = override != null ? override : resolveSerializer(state);
        return ser != null ? ser.getField(idx) : null;
    }

    @Override
    public Integer getChildIndex(S2EntityState state, String nameSegment) {
        var ser = resolveSerializer(state);
        return ser != null ? ser.getFieldIndex(nameSegment) : null;
    }

    @Override
    public String getChildNameSegment(S2EntityState state, int idx) {
        var ser = resolveSerializer(state);
        return ser != null ? ser.getFieldName(idx) : null;
    }

    @Override
    public StateMutation createMutation(Object decodedValue, int depth) {
        return new StateMutation.SwitchPointer((Serializer) prepareForWrite(decodedValue, depth));
    }

    @Override
    public Object prepareForWrite(Object decodedValue, int depth) {
        var p = (Pointer) decodedValue;
        var typeIndex = p.getTypeIndex();
        return typeIndex != null ? serializers[typeIndex] : null;
    }

}
