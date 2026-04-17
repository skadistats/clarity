package skadistats.clarity.model.s2;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.state.StateMutation;
import skadistats.clarity.state.s2.S2EntityState;

public abstract class Field {

    private final FieldType fieldType;

    public Field(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public FieldType getType() {
        return fieldType;
    }

    public SerializerProperties getSerializerProperties() {
        return SerializerProperties.DEFAULT;
    }

    public Decoder getDecoder() {
        return null;
    }

    public Field getChild(S2EntityState state, int idx) {
        return null;
    }

    public Integer getChildIndex(S2EntityState state, String nameSegment) {
        return null;
    }

    public String getChildNameSegment(S2EntityState state, int idx) {
        return null;
    }

    public boolean isHiddenFieldPath() {
        return false;
    }

    public boolean isPrimitiveLeaf() {
        return false;
    }

    public StateMutation createMutation(Object decodedValue, int depth) {
        return new StateMutation.WriteValue(decodedValue);
    }

    // Transforms the raw decoded value into the form state.write expects for
    // this field's leaf kind. Default is identity (primitive / ref leaves).
    // PointerField overrides to resolve Pointer -> Serializer; VectorField
    // validates the count.
    public Object prepareForWrite(Object decodedValue, int depth) {
        return decodedValue;
    }

    public String toString() {
        return getType().toString();
    }

}
