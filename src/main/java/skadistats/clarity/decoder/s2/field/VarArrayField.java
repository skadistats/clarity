package skadistats.clarity.decoder.s2.field;

import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.s2.DumpEntry;
import skadistats.clarity.decoder.s2.S2UnpackerFactory;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.FieldPath;

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
        return null;
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
        assert fp.last == pos || fp.last == pos - 1;
        if (pos == fp.last) {
            return elementUnpacker;
        } else {
            return baseUnpacker;
        }
    }

    @Override
    public Field getFieldForFieldPath(FieldPath fp, int pos) {
        assert fp.last == pos || fp.last == pos - 1;
        return this;
    }

    @Override
    public FieldType getTypeForFieldPath(FieldPath fp, int pos) {
        assert fp.last == pos || fp.last == pos - 1;
        if (pos == fp.last) {
            return properties.getType().getGenericType();
        } else {
            return properties.getType();
        }
    }

    @Override
    public Object getValueForFieldPath(FieldPath fp, int pos, Object[] state) {
        assert fp.last == pos;
        Object[] subState = (Object[]) state[fp.path[pos - 1]];
        return subState[fp.path[pos]];
    }

    @Override
    public void setValueForFieldPath(FieldPath fp, int pos, Object[] state, Object value) {
        assert fp.last == pos || fp.last == pos - 1;
        int i = fp.path[pos - 1];
        Object[] subState = (Object[]) state[i];
        if (fp.last == pos) {
            subState[fp.path[pos]] = value;
        } else {
            int size = ((Integer) value).intValue();
            int curSize = subState == null ? 0 : subState.length;
            if (subState == null && size > 0) {
                state[i] = new Object[size];
            } else if (subState != null && size == 0) {
                state[i] = null;
            } else if (curSize != size) {
                state[i] = new Object[size];
                System.arraycopy(subState, 0, state[i], 0, Math.min(subState.length, size));
            }
        }
    }

    @Override
    public FieldPath getFieldPathForName(FieldPath fp, String property) {
        if (property.length() != 4) {
            throw new RuntimeException("unresolvable fieldpath");
        }
        fp.path[fp.last] = Integer.valueOf(property);
        return fp;
    }

    @Override
    public void collectDump(FieldPath fp, String namePrefix, List<DumpEntry> entries, Object[] state) {
        Object[] subState = (Object[]) state[fp.path[fp.last]];
        fp.last++;
        for (int i = 0; i < subState.length; i++) {
            if (subState[i] != null) {
                fp.path[fp.last] = i;
                entries.add(new DumpEntry(fp, joinPropertyName(namePrefix, properties.getName(), Util.arrayIdxToString(i)), subState[i]));
            }
        }
        fp.last--;
    }

}
