package skadistats.clarity.decoder.s2.field;

import skadistats.clarity.decoder.s2.DumpEntry;
import skadistats.clarity.decoder.s2.S2UnpackerFactory;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.EntityState;

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
    public Object getValueForFieldPath(FieldPath fp, int pos, EntityState state) {
        assert fp.last >= pos;
        int i = fp.path[pos];
        if (fp.last == pos) {
            return state.has(i);
        } else {
            return properties.getSerializer().getValueForFieldPath(fp, pos + 1, (EntityState) state.get(i));
        }
    }

    @Override
    public void setValueForFieldPath(FieldPath fp, int pos, EntityState state, Object value) {
        assert fp.last >= pos;
        int i = fp.path[pos];
        EntityState subState = state.sub(i);
        if (fp.last == pos) {
            boolean existing = (Boolean) value;
            if (subState == null && existing) {
                state.set(i, properties.getSerializer().getInitialState());
            } else if (subState != null && !existing) {
                state.set(i, null);
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
    public void collectDump(FieldPath fp, String namePrefix, List<DumpEntry> entries, EntityState state) {
        String name = joinPropertyName(namePrefix, properties.getName());
        int i = fp.path[fp.last];
        if (state.has(i)) {
            fp.last++;
            properties.getSerializer().collectDump(fp, name, entries, state.sub(i));
            fp.last--;
        }
    }

    @Override
    public void collectFieldPaths(FieldPath fp, List<FieldPath> entries, EntityState state) {
        int i = fp.path[fp.last];
        if (state.has(i)) {
            fp.last++;
            properties.getSerializer().collectFieldPaths(fp, entries, state.sub(i));
            fp.last--;
        }

    }
}
