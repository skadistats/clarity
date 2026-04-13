package skadistats.clarity.io.s2;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.model.state.StateMutation;

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

    public Field getChild(int idx) {
        return null;
    }

    public Integer getChildIndex(String nameSegment) {
        return null;
    }

    public String getChildNameSegment(int idx) {
        return null;
    }

    public boolean isHiddenFieldPath() {
        return false;
    }

    public StateMutation createMutation(Object decodedValue, int depth) {
        return new StateMutation.WriteValue(decodedValue);
    }

    public String toString() {
        return getType().toString();
    }

}
