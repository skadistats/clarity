package skadistats.clarity.model.s2.field;

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
    public void accumulateName(List<String> parts, FieldPath fp, int pos) {
        addBasePropertyName(parts);
        if (fp.last != pos) {
            assertFieldPathEnd(fp, pos + 1);
            parts.add(Util.arrayIdxToString(fp.path[++pos]));
        }
    }

    @Override
    public Unpacker queryUnpacker(FieldPath fp, int pos) {
        assertFieldPathEnd(fp, pos + 1);
        return elementUnpacker;
    }

    @Override
    public Field queryField(FieldPath fp, int pos) {
        assertFieldPathEnd(fp, pos + 1);
        return this;
    }

    @Override
    public FieldType queryType(FieldPath fp, int pos) {
        assertFieldPathEnd(fp, pos + 1);
        return properties.getType();
    }

    @Override
    public void setValueForFieldPath(FieldPath fp, Object[] state, Object data, int pos) {
        assertFieldPathEnd(fp, pos + 1);
        Object[] myState = (Object[]) state[fp.path[pos]];
        myState[fp.path[pos + 1]] = data;
    }

    @Override
    public Object getValueForFieldPath(FieldPath fp, Object[] state, int pos) {
        assertFieldPathEnd(fp, pos + 1);
        Object[] myState = (Object[]) state[fp.path[pos]];
        return myState[fp.path[pos + 1]];
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
