package skadistats.clarity.io.s2.field;

import skadistats.clarity.io.Util;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.s2.DecoderHolder;
import skadistats.clarity.io.s2.DecoderProperties;
import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.FieldType;
import skadistats.clarity.io.s2.S2DecoderFactory;
import skadistats.clarity.model.state.ArrayEntityState;

public class ListField extends Field {

    private static final DecoderHolder decoderHolder = S2DecoderFactory.createDecoder("uint32");

    private final Field elementField;

    public ListField(FieldType fieldType, Field elementField) {
        super(fieldType);
        this.elementField = elementField;
    }

    @Override
    public DecoderProperties getDecoderProperties() {
        return decoderHolder.getDecoderProperties();
    }

    @Override
    public Decoder<?> getDecoder() {
        return decoderHolder.getDecoder();
    }

    @Override
    public Field getChild(int idx) {
        return elementField;
    }

    @Override
    public Integer getChildIndex(String name) {
        return Util.stringToArrayIdx(name);
    }

    @Override
    public String getChildNameSegment(int idx) {
        return Util.arrayIdxToString(idx);
    }

    @Override
    public void ensureArrayEntityStateCapacity(ArrayEntityState state, int capacity) {
        state.capacity(capacity, false);
    }

    @Override
    public void setArrayEntityState(ArrayEntityState state, int idx, Object value) {
        state.sub(idx).capacity((Integer) value, true);
    }

    @Override
    public Object getArrayEntityState(ArrayEntityState state, int idx) {
        return state.sub(idx).length();
    }

}
