package skadistats.clarity.decoder.s2.field;

import skadistats.clarity.decoder.s2.DumpEntry;
import skadistats.clarity.decoder.s2.S2UnpackerFactory;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.FieldPath;

import java.util.List;

public class FixedSubTableField extends Field {

    private final Unpacker baseUnpacker;

    public FixedSubTableField(FieldProperties properties) {
        super(properties);
        baseUnpacker = S2UnpackerFactory.createUnpacker(properties, "bool");
    }

    @Override
    public Object getInitialState() {
        return properties.getSerializer().getInitialState();
    }

    @Override
    public void accumulateName(FieldPath fp, int pos, List<String> parts) {
        assert fp.last >= pos;
        addBasePropertyName(parts);
        if (fp.last > pos) {
            properties.getSerializer().accumulateName(fp, pos + 1, parts);
        }
    }

    @Override
    public Unpacker getUnpackerForFieldPath(FieldPath fp, int pos) {
        assert fp.last >= pos;
        if (fp.last == pos) {
            return baseUnpacker;
        } else {
            return properties.getSerializer().getUnpackerForFieldPath(fp, pos + 1);
        }
    }

    @Override
    public Field getFieldForFieldPath(FieldPath fp, int pos) {
        assert fp.last >= pos;
        if (fp.last == pos) {
            return this;
        } else {
            return properties.getSerializer().getFieldForFieldPath(fp, pos + 1);
        }
    }

    @Override
    public FieldType getTypeForFieldPath(FieldPath fp, int pos) {
        assert fp.last >= pos;
        if (fp.last == pos) {
            return properties.getType();
        } else {
            return properties.getSerializer().getTypeForFieldPath(fp, pos + 1);
        }
    }

    @Override
    public Object getValueForFieldPath(FieldPath fp, int pos, Object[] state) {
        assert fp.last >= pos;
        int i = fp.path[pos];
        Object[] subState = (Object[]) state[i];
        if (fp.last == pos) {
            return subState != null;
        } else {
            return properties.getSerializer().getValueForFieldPath(fp, pos + 1, subState);
        }
    }

    @Override
    public void setValueForFieldPath(FieldPath fp, int pos, Object[] state, Object value) {
        assert fp.last >= pos;
        int i = fp.path[pos];
        Object[] subState = (Object[]) state[i];
        if (fp.last == pos) {
            boolean existing = ((Boolean) value).booleanValue();
            if (subState == null && existing) {
                state[i] = properties.getSerializer().getInitialState();
            } else if (subState != null && !existing) {
                state[i] = null;
            }
        } else {
            properties.getSerializer().setValueForFieldPath(fp, pos + 1, subState, value);
        }
    }

    @Override
    public FieldPath getFieldPathForName(FieldPath fp, String property) {
        return properties.getSerializer().getFieldPathForName(fp, property);
    }

    @Override
    public void collectDump(FieldPath fp, String namePrefix, List<DumpEntry> entries, Object[] state) {
        Object[] subState = (Object[]) state[fp.path[fp.last]];
        String name = joinPropertyName(namePrefix, properties.getName());
        if (subState != null) {
            fp.last++;
            properties.getSerializer().collectDump(fp, name, entries, subState);
            fp.last--;
        }
    }

    @Override
    public void collectFieldPaths(FieldPath fp, List<FieldPath> entries, Object[] state) {
        Object[] subState = (Object[]) state[fp.path[fp.last]];
        if (subState != null) {
            fp.last++;
            properties.getSerializer().collectFieldPaths(fp, entries, subState);
            fp.last--;
        }

    }
}
