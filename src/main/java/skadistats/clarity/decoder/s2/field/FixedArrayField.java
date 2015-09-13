package skadistats.clarity.decoder.s2.field;

import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.s2.S2UnpackerFactory;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.FieldPath;

import java.util.List;

public class FixedArrayField extends Field {

    private final int length;
    private final Unpacker elementUnpacker;

    public FixedArrayField(FieldProperties properties, int length) {
        super(properties);
        this.length = length;
        elementUnpacker = S2UnpackerFactory.createUnpacker(properties, properties.getType().getBaseType());
    }

    @Override
    public Object getInitialState() {
        return new Object[length];
    }

    @Override
    public void accumulateName(FieldPath fp, int pos, List<String> parts) {
        assert fp.last == pos || fp.last == pos - 1;
        addBasePropertyName(parts);
        if (fp.last == pos) {
            parts.add(Util.arrayIdxToString(fp.path[pos]));
        }
    }

    @Override
    public Unpacker getUnpackerForFieldPath(FieldPath fp, int pos) {
        assert fp.last == pos;
        return elementUnpacker;
    }

    @Override
    public Field getFieldForFieldPath(FieldPath fp, int pos) {
        assert fp.last == pos;
        return this;
    }

    @Override
    public FieldType getTypeForFieldPath(FieldPath fp, int pos) {
        assert fp.last == pos;
        return properties.getType();
    }

    @Override
    public void setValueForFieldPath(FieldPath fp, int pos, Object[] state, Object value) {
        assert fp.last == pos;
        Object[] subState = (Object[]) state[fp.path[pos - 1]];
        subState[fp.path[pos]] = value;
    }

    @Override
    public Object getValueForFieldPath(FieldPath fp, int pos, Object[] state) {
        assert fp.last == pos;
        Object[] subState = (Object[]) state[fp.path[pos - 1]];
        return subState[fp.path[pos]];
    }

    @Override
    public FieldPath getFieldPathForName(FieldPath fp, String property) {
        if (property.length() != 4) {
            throw new RuntimeException("unresolvable fieldpath");
        }
        fp.path[fp.last] = Integer.valueOf(property);
        return fp;
    }

}
