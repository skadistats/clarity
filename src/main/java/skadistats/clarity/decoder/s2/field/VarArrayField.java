package skadistats.clarity.decoder.s2.field;

import skadistats.clarity.ClarityException;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.s2.S2UnpackerFactory;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.ArrayEntityState;

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
    public void initInitialState(ArrayEntityState state, int idx) {
        state.set(idx, null);
    }

    @Override
    public void accumulateName(FieldPath fp, int pos, List<String> parts) {
        assert fp.last() == pos || fp.last() == pos + 1;
        addBasePropertyName(parts);
        if (fp.last() > pos) {
            parts.add(Util.arrayIdxToString(fp.get(pos + 1)));
        }
    }

    @Override
    public Unpacker getUnpackerForFieldPath(FieldPath fp, int pos) {
        assert fp.last() == pos || fp.last() == pos + 1;
        if (pos == fp.last()) {
            return baseUnpacker;
        } else {
            return elementUnpacker;
        }
    }

    @Override
    public Field getFieldForFieldPath(FieldPath fp, int pos) {
        assert fp.last() == pos || fp.last() == pos + 1;
        return this;
    }

    @Override
    public FieldType getTypeForFieldPath(FieldPath fp, int pos) {
        assert fp.last() == pos || fp.last() == pos + 1;
        if (fp.last() == pos) {
            return properties.getType();
        } else {
            return properties.getType().getGenericType();
        }
    }

    @Override
    public Object getValueForFieldPath(FieldPath fp, int pos, ArrayEntityState state) {
        assert fp.last() == pos || fp.last() == pos + 1;
        ArrayEntityState subState = state.sub(fp.get(pos));
        if (fp.last() == pos) {
            return subState.length();
        } else if (subState.has(fp.get(pos + 1))) {
            return subState.get(fp.get(pos + 1));
        } else {
            return null;
        }
    }

    @Override
    public void setValueForFieldPath(FieldPath fp, int pos, ArrayEntityState state, Object value) {
        assert fp.last() == pos || fp.last() == pos + 1;
        int i = fp.get(pos);
        ArrayEntityState subState = state.sub(i);
        if (fp.last() == pos) {
            subState.capacity((Integer) value, true);
        } else {
            int j = fp.get(pos + 1);
            subState.capacity(j + 1, false);
            subState.set(j, value);
        }
    }

    @Override
    public FieldPath getFieldPathForName(FieldPath fp, String property) {
        if (property.length() != 4) {
            throw new ClarityException("unresolvable fieldpath");
        }
        fp.cur(Integer.parseInt(property));
        return fp;
    }

    @Override
    public void collectFieldPaths(FieldPath fp, List<FieldPath> entries, ArrayEntityState state) {
        ArrayEntityState subState = state.sub(fp.cur());
        fp.down();
        for (int i = 0; i < subState.length(); i++) {
            if (subState.has(i)) {
                fp.cur(i);
                entries.add(new FieldPath(fp));
            }
        }
        fp.up(1);
    }

}
