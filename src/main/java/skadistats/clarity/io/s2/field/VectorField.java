package skadistats.clarity.io.s2.field;

import skadistats.clarity.ClarityException;
import skadistats.clarity.io.Util;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.s2.DecoderHolder;
import skadistats.clarity.io.s2.DecoderProperties;
import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.FieldType;
import skadistats.clarity.io.s2.S2DecoderFactory;
import skadistats.clarity.model.s2.S2LongFieldPathFormat;
import skadistats.clarity.model.state.ArrayEntityState;

public class VectorField extends Field {

    private static final DecoderHolder decoderHolder = S2DecoderFactory.createDecoder("uint32");

    private final Field elementField;

    public VectorField(FieldType fieldType, Field elementField) {
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
    public Integer getChildIndex(String nameSegment) {
        return Util.stringToArrayIdx(nameSegment);
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
    public boolean isHiddenFieldPath() {
        return true;
    }

    @Override
    public void setArrayEntityState(ArrayEntityState state, int idx, int childDepth, Object value) {
        var count = (Integer) value;
        // A vector's elements live at childDepth in the field path. The
        // FieldPath format imposes a hard cap on indices at every depth
        // (see S2LongFieldPathFormat). A length larger than that is
        // structurally unaddressable and therefore a decoder desync,
        // almost always caused by an unknown field type earlier in the
        // stream falling back to the wrong decoder.
        var maxLength = S2LongFieldPathFormat.maxIndexAtDepth(childDepth) + 1;
        if (count < 0 || count > maxLength) {
            throw new ClarityException(
                "decoder desync: vector length %d for field %s at depth %d exceeds the structural maximum of %d. " +
                "This usually means an unknown field type earlier in the stream was decoded with the wrong decoder.",
                count, getType(), childDepth, maxLength
            );
        }
        state.sub(idx).capacity(count, true);
    }

    @Override
    public Object getArrayEntityState(ArrayEntityState state, int idx) {
        return state.sub(idx).length();
    }

}
