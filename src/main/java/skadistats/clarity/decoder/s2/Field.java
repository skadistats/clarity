package skadistats.clarity.decoder.s2;

import skadistats.clarity.decoder.s2.field.FieldType;
import skadistats.clarity.decoder.s2.field.DecoderProperties;
import skadistats.clarity.decoder.unpacker.Decoder;
import skadistats.clarity.model.state.ArrayEntityState;

public abstract class Field {

    private final FieldType fieldType;
    protected final DecoderProperties decoderProperties;

    public Field(FieldType fieldType, DecoderProperties decoderProperties) {
        this.fieldType = fieldType;
        this.decoderProperties = decoderProperties;
    }

    public DecoderProperties getDecoderProperties() {
        return decoderProperties;
    }

    public String getChildNameSegment(int idx) {
        return null;
    }

    public FieldType getType() {
        return fieldType;
    }

    public Decoder<?> getDecoder() {
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
