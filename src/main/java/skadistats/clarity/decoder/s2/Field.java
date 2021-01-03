package skadistats.clarity.decoder.s2;

import skadistats.clarity.decoder.s2.field.FieldProperties;
import skadistats.clarity.decoder.s2.field.FieldType;
import skadistats.clarity.decoder.s2.field.UnpackerProperties;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.state.ArrayEntityState;

public abstract class Field {

    protected final FieldProperties fieldProperties;
    protected final UnpackerProperties unpackerProperties;

    public Field(FieldProperties fieldProperties, UnpackerProperties unpackerProperties) {
        this.fieldProperties = fieldProperties;
        this.unpackerProperties = unpackerProperties;
    }

    public FieldProperties getFieldProperties() {
        return fieldProperties;
    }

    public UnpackerProperties getUnpackerProperties() {
        return unpackerProperties;
    }

    public FieldType getType() {
        return fieldProperties.getType();
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
        return fieldProperties.getName() + "(" + fieldProperties.getType() + ")";
    }

}
