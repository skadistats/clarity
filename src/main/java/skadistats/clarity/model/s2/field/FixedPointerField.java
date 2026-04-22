package skadistats.clarity.model.s2.field;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.FixedPointerDecoder;
import skadistats.clarity.model.s2.Field;
import skadistats.clarity.model.s2.FieldType;
import skadistats.clarity.model.s2.Serializer;
import skadistats.clarity.model.s2.SerializerProperties;
import skadistats.clarity.state.StateMutation;
import skadistats.clarity.state.s2.S2EntityState;

public final class FixedPointerField extends Field {

    private static final Decoder DECODER = new FixedPointerDecoder();

    private final SerializerProperties serializerProperties;
    private final Serializer serializer;

    public FixedPointerField(FieldType fieldType, SerializerProperties serializerProperties, Serializer serializer) {
        super(fieldType);
        this.serializerProperties = serializerProperties;
        this.serializer = serializer;
    }

    public FixedPointerField(FieldType fieldType, Serializer serializer) {
        super(fieldType);
        this.serializerProperties = null;
        this.serializer = serializer;
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
        return DECODER;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    @Override
    public Field getChild(S2EntityState state, int idx) {
        return serializer.getField(idx);
    }

    @Override
    public Integer getChildIndex(S2EntityState state, String nameSegment) {
        return serializer.getFieldIndex(nameSegment);
    }

    @Override
    public String getChildNameSegment(S2EntityState state, int idx) {
        return serializer.getFieldName(idx);
    }

    @Override
    public StateMutation createMutation(Object decodedValue, int depth) {
        return new StateMutation.SwitchFixedPointer((Boolean) decodedValue ? serializer : null);
    }

    @Override
    public Object prepareForWrite(Object decodedValue, int depth) {
        return (Boolean) decodedValue ? serializer : null;
    }

}
