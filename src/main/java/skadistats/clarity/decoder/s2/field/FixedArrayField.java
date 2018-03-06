package skadistats.clarity.decoder.s2.field;

import skadistats.clarity.ClarityException;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.s2.DumpEntry;
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
        assert fp.last == pos || fp.last == pos + 1;
        addBasePropertyName(parts);
        if (fp.last > pos) {
            parts.add(Util.arrayIdxToString(fp.path[pos + 1]));
        }
    }

    @Override
    public Unpacker getUnpackerForFieldPath(FieldPath fp, int pos) {
        assert fp.last == pos || fp.last == pos + 1;
        if (fp.last == pos) {
            return null;
        } else {
            return elementUnpacker;
        }
    }

    @Override
    public Field getFieldForFieldPath(FieldPath fp, int pos) {
        assert fp.last == pos || fp.last == pos + 1;
        return this;
    }

    @Override
    public FieldType getTypeForFieldPath(FieldPath fp, int pos) {
        assert fp.last == pos || fp.last == pos + 1;
        if (fp.last == pos) {
            return properties.getType();
        } else {
            return properties.getType().getElementType();
        }
    }

    @Override
    public Object getValueForFieldPath(FieldPath fp, int pos, Object[] state) {
        assert fp.last == pos || fp.last == pos + 1;
        Object[] subState = (Object[]) state[fp.path[pos]];
        if (fp.last == pos) {
            return subState.length;
        } else {
            return subState[fp.path[pos + 1]];
        }
    }

    @Override
    public void setValueForFieldPath(FieldPath fp, int pos, Object[] state, Object value) {
        assert fp.last == pos || fp.last == pos + 1;
        if (fp.last == pos) {
            throw new ClarityException("base of a FixedArrayField cannot be set");
        } else {
            Object[] subState = (Object[]) state[fp.path[pos]];
            subState[fp.path[pos + 1]] = value;
        }
    }

    @Override
    public FieldPath getFieldPathForName(FieldPath fp, String property) {
        if (property.length() != 4) {
            throw new ClarityException("unresolvable fieldpath");
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

    @Override
    public void collectFieldPaths(FieldPath fp, List<FieldPath> entries, Object[] state) {
        Object[] subState = (Object[]) state[fp.path[fp.last]];
        fp.last++;
        for (int i = 0; i < subState.length; i++) {
            if (subState[i] != null) {
                fp.path[fp.last] = i;
                entries.add(new FieldPath(fp));
            }
        }
        fp.last--;
    }

}
