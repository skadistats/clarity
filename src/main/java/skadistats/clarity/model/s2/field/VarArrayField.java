package skadistats.clarity.model.s2.field;

import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.s2.S2UnpackerFactory;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.s2.FieldPath;

import java.util.List;

public class VarArrayField extends Field {

    private final Unpacker baseUnpacker;
    private final Unpacker elementUnpacker;

    public VarArrayField(FieldProperties properties) {
        super(properties);
        baseUnpacker = S2UnpackerFactory.createUnpacker(properties, "uint32");
        elementUnpacker = S2UnpackerFactory.createUnpacker(properties, properties.getType().getGenericType().getBaseType());
    }

    @Override
    public Object getInitialState() {
        return new Object[] { 0, new Object[0] };
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
        if (pos == fp.last) {
            return baseUnpacker;
        } else {
            assertFieldPathEnd(fp, pos + 1);
            return elementUnpacker;
        }
    }

    @Override
    public Field queryField(FieldPath fp, int pos) {
        if (pos == fp.last) {
            return this;
        } else {
            assertFieldPathEnd(fp, pos + 1);
            return this;
        }
    }

    @Override
    public FieldType queryType(FieldPath fp, int pos) {
        if (pos == fp.last) {
            return properties.getType();
        } else {
            assertFieldPathEnd(fp, pos + 1);
            return properties.getType().getGenericType();
        }
    }

}
