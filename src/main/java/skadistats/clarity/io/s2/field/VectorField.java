package skadistats.clarity.io.s2.field;

import skadistats.clarity.ClarityException;
import skadistats.clarity.io.Util;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.IntVarUnsignedDecoder;
import skadistats.clarity.io.s2.Field;
import skadistats.clarity.io.s2.FieldType;
import skadistats.clarity.io.s2.SerializerProperties;
import skadistats.clarity.model.s2.S2LongFieldPathFormat;
import skadistats.clarity.model.state.AbstractS2EntityState;
import skadistats.clarity.model.state.StateMutation;

public class VectorField extends Field {

    private static final IntVarUnsignedDecoder LENGTH_DECODER = new IntVarUnsignedDecoder();

    private final Field elementField;

    public VectorField(FieldType fieldType, Field elementField) {
        super(fieldType);
        this.elementField = elementField;
    }

    public Field getElementField() {
        return elementField;
    }

    @Override
    public boolean isHiddenFieldPath() {
        return true;
    }

    @Override
    public SerializerProperties getSerializerProperties() {
        return SerializerProperties.DEFAULT;
    }

    @Override
    public Decoder getDecoder() {
        return LENGTH_DECODER;
    }

    @Override
    public Field getChild(AbstractS2EntityState state, int idx) {
        return elementField;
    }

    @Override
    public Integer getChildIndex(AbstractS2EntityState state, String nameSegment) {
        return Util.stringToArrayIdx(nameSegment);
    }

    @Override
    public String getChildNameSegment(AbstractS2EntityState state, int idx) {
        return Util.arrayIdxToString(idx);
    }

    @Override
    public StateMutation createMutation(Object decodedValue, int depth) {
        var count = (Integer) decodedValue;
        var maxLength = S2LongFieldPathFormat.maxIndexAtDepth(depth) + 1;
        if (count < 0 || count > maxLength) {
            throw new ClarityException(
                "decoder desync: vector length %d for field %s at depth %d exceeds the structural maximum of %d. " +
                "This usually means an unknown field type earlier in the stream was decoded with the wrong decoder.",
                count, getType(), depth, maxLength
            );
        }
        return new StateMutation.ResizeVector(count);
    }

}
