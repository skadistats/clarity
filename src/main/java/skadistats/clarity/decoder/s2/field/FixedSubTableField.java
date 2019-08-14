package skadistats.clarity.decoder.s2.field;

import skadistats.clarity.decoder.s2.S2UnpackerFactory;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.state.ArrayEntityState;

import java.util.List;

public class FixedSubTableField extends Field {

    private final Unpacker baseUnpacker;

    public FixedSubTableField(FieldProperties properties) {
        super(properties);
        baseUnpacker = S2UnpackerFactory.createUnpacker(properties, "bool");
    }

    @Override
    public void initInitialState(ArrayEntityState state, int idx) {
        properties.getSerializer().initInitialState(state.sub(idx));
    }

    @Override
    public void accumulateName(S2FieldPath fp, int pos, List<String> parts) {
        assert fp.last() >= pos;
        addBasePropertyName(parts);
        if (fp.last() > pos) {
            properties.getSerializer().accumulateName(fp, pos + 1, parts);
        }
    }

    @Override
    public Unpacker getUnpackerForFieldPath(S2FieldPath fp, int pos) {
        assert fp.last() >= pos;
        if (fp.last() == pos) {
            return baseUnpacker;
        } else {
            return properties.getSerializer().getUnpackerForFieldPath(fp, pos + 1);
        }
    }

    @Override
    public Field getFieldForFieldPath(S2FieldPath fp, int pos) {
        assert fp.last() >= pos;
        if (fp.last() == pos) {
            return this;
        } else {
            return properties.getSerializer().getFieldForFieldPath(fp, pos + 1);
        }
    }

    @Override
    public FieldType getTypeForFieldPath(S2FieldPath fp, int pos) {
        assert fp.last() >= pos;
        if (fp.last() == pos) {
            return properties.getType();
        } else {
            return properties.getSerializer().getTypeForFieldPath(fp, pos + 1);
        }
    }

    @Override
    public Object getValueForFieldPath(S2FieldPath fp, int pos, ArrayEntityState state) {
        assert fp.last() >= pos;
        int i = fp.get(pos);
        if (fp.last() == pos) {
            return state.has(i);
        } else if (state.isSub(i)) {
            return properties.getSerializer().getValueForFieldPath(fp, pos + 1, state.sub(i));
        } else {
            return null;
        }
    }

    @Override
    public void setValueForFieldPath(S2FieldPath fp, int pos, ArrayEntityState state, Object value) {
        assert fp.last() >= pos;
        int i = fp.get(pos);
        if (fp.last() == pos) {
            boolean existing = (Boolean) value;
            if (!state.has(i) && existing) {
                properties.getSerializer().initInitialState(state.sub(i));
            } else if (state.has(i) && !existing) {
                state.clear(i);
            }
        } else {
            properties.getSerializer().setValueForFieldPath(fp, pos + 1, state.sub(i), value);
        }
    }

    @Override
    public S2FieldPath getFieldPathForName(S2FieldPath fp, String property) {
        return properties.getSerializer().getFieldPathForName(fp, property);
    }

    @Override
    public void collectFieldPaths(S2FieldPath fp, List<FieldPath> entries, ArrayEntityState state) {
        int i = fp.cur();
        if (state.has(i)) {
            fp.down();
            properties.getSerializer().collectFieldPaths(fp, entries, state.sub(i));
            fp.up(1);
        }

    }
}
