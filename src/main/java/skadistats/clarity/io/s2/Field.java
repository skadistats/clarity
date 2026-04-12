package skadistats.clarity.io.s2;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.model.state.NestedEntityState;

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

    public Object getValue(NestedEntityState state, int idx) {
        return null;
    }

    public void setValue(NestedEntityState state, int idx, int childDepth, Object value) {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public void ensureCapacity(NestedEntityState state, int capacity) {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public boolean isHiddenFieldPath() {
        // true, if this field's state value is hidden
        return false;
    }

    public String toString() {
        return getType().toString();
    }

}
