package skadistats.clarity.decoder.s2;

import skadistats.clarity.decoder.s2.field.FieldType;
import skadistats.clarity.decoder.s2.field.UnpackerProperties;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.state.ArrayEntityState;

public abstract class Field {

    private final FieldType fieldType;
    protected final UnpackerProperties unpackerProperties;

    public Field(FieldType fieldType, UnpackerProperties unpackerProperties) {
        this.fieldType = fieldType;
        this.unpackerProperties = unpackerProperties;
    }

    public UnpackerProperties getUnpackerProperties() {
        return unpackerProperties;
    }

    public String getChildNameSegment(int idx) {
        return null;
    }

    public FieldType getType() {
        return fieldType;
    }

    public Unpacker<?> getUnpacker() {
        return null;
    }

    public Field getChild(int idx) {
        return null;
    }

    public Integer getChildIndex(String name) {
        return null;
    }

    public Object getArrayEntityState(ArrayEntityState state, int idx) {
        return null;
    }

    public void setArrayEntityState(ArrayEntityState state, int idx, Object value) {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public void ensureArrayEntityStateCapacity(ArrayEntityState state, int capacity) {
        throw new UnsupportedOperationException(getClass().getSimpleName());
    }

    public String toString() {
        return "Field (" + getType() + ")";
    }

}
