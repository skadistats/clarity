package skadistats.clarity.decoder.s2.field;

import skadistats.clarity.ClarityException;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.s2.DumpEntry;
import skadistats.clarity.decoder.s2.S2UnpackerFactory;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.FieldPath;

import java.util.List;

public class VarSubTableField extends Field {

    private final Unpacker baseUnpacker;

    public VarSubTableField(FieldProperties properties) {
        super(properties);
        baseUnpacker = S2UnpackerFactory.createUnpacker(properties, "uint32");
    }

    @Override
    public Object getInitialState() {
        return null;
    }

    @Override
    public void accumulateName(FieldPath fp, int pos, List<String> parts) {
        assert fp.last == pos || fp.last >= pos + 2;
        addBasePropertyName(parts);
        if (fp.last > pos) {
            parts.add(Util.arrayIdxToString(fp.path[pos + 1]));
            if (fp.last > pos + 1) {
                properties.getSerializer().accumulateName(fp, pos + 2, parts);
            }
        }
    }

    @Override
    public Unpacker getUnpackerForFieldPath(FieldPath fp, int pos) {
        assert fp.last == pos || fp.last >= pos + 2;
        if (fp.last == pos) {
            return baseUnpacker;
        } else {
            return properties.getSerializer().getUnpackerForFieldPath(fp, pos + 2);
        }
    }

    @Override
    public Field getFieldForFieldPath(FieldPath fp, int pos) {
        assert fp.last == pos || fp.last >= pos + 2;
        if (fp.last == pos) {
            return this;
        } else {
            return properties.getSerializer().getFieldForFieldPath(fp, pos + 2);
        }
    }

    @Override
    public FieldType getTypeForFieldPath(FieldPath fp, int pos) {
        assert fp.last == pos || fp.last >= pos + 2;
        if (fp.last == pos) {
            return properties.getType();
        } else {
            return properties.getSerializer().getTypeForFieldPath(fp, pos + 2);
        }
    }

    @Override
    public Object getValueForFieldPath(FieldPath fp, int pos, Object[] state) {
        assert fp.last == pos || fp.last >= pos + 2;
        Object[] subState = (Object[]) state[fp.path[pos]];
        if (fp.last == pos) {
            return subState.length;
        } else {
            return properties.getSerializer().getValueForFieldPath(fp, pos + 2, (Object[]) subState[fp.path[pos + 1]]);
        }
    }

    @Override
    public void setValueForFieldPath(FieldPath fp, int pos, Object[] state, Object value) {
        assert fp.last == pos || fp.last >= pos + 2;
        int i = fp.path[pos];
        if (fp.last == pos) {
            ensureSubStateCapacity(state, i, (Integer) value, true);
        } else {
            int j = fp.path[pos + 1];
            Object[] subState = ensureSubStateCapacity(state, i, j + 1, false);
            properties.getSerializer().setValueForFieldPath(fp, pos + 2, (Object[]) subState[j], value);
        }
    }

    @Override
    public FieldPath getFieldPathForName(FieldPath fp, String property) {
        if (property.length() < 5) {
            throw new ClarityException("unresolvable fieldpath");
        }
        String idx = property.substring(0, 4);
        fp.path[fp.last] = Integer.valueOf(idx);
        fp.last++;
        return properties.getSerializer().getFieldPathForName(fp, property.substring(5));
    }

    @Override
    public void collectDump(FieldPath fp, String namePrefix, List<DumpEntry> entries, Object[] state) {
        Object[] subState = (Object[]) state[fp.path[fp.last]];
        String name = joinPropertyName(namePrefix, properties.getName());
        if (subState.length > 0) {
            entries.add(new DumpEntry(fp, name, subState.length));
            fp.last += 2;
            for (int i = 0; i < subState.length; i++) {
                fp.path[fp.last - 1] = i;
                properties.getSerializer().collectDump(fp, joinPropertyName(name, Util.arrayIdxToString(i)), entries, (Object[])subState[i]);
            }
            fp.last -= 2;
        }
    }

    @Override
    public void collectFieldPaths(FieldPath fp, List<FieldPath> entries, Object[] state) {
        Object[] subState = (Object[]) state[fp.path[fp.last]];
        if (subState.length > 0) {
            fp.last += 2;
            for (int i = 0; i < subState.length; i++) {
                fp.path[fp.last - 1] = i;
                properties.getSerializer().collectFieldPaths(fp, entries, (Object[])subState[i]);
            }
            fp.last -= 2;
        }
    }

}
