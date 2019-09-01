package skadistats.clarity.decoder.s2.field;

import skadistats.clarity.decoder.s2.S2UnpackerFactory;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.S2ModifiableFieldPath;
import skadistats.clarity.model.state.ArrayEntityState;

import java.util.List;

public class FixedSubTableField extends Field {

    private final Unpacker baseUnpacker;

    public FixedSubTableField(FieldProperties fieldProperties, UnpackerProperties unpackerProperties) {
        super(fieldProperties, unpackerProperties);
        baseUnpacker = S2UnpackerFactory.createUnpacker(unpackerProperties, "bool");
    }

    @Override
    public void accumulateName(S2FieldPath fp, int pos, List<String> parts) {
        assert fp.last() >= pos;
        addBasePropertyName(parts);
        if (fp.last() > pos) {
            unpackerProperties.getSerializer().accumulateName(fp, pos + 1, parts);
        }
    }

    @Override
    public Unpacker getUnpackerForFieldPath(S2FieldPath fp, int pos) {
        assert fp.last() >= pos;
        if (fp.last() == pos) {
            return baseUnpacker;
        } else {
            return unpackerProperties.getSerializer().getUnpackerForFieldPath(fp, pos + 1);
        }
    }

    @Override
    public Field getFieldForFieldPath(S2FieldPath fp, int pos) {
        assert fp.last() >= pos;
        if (fp.last() == pos) {
            return this;
        } else {
            return unpackerProperties.getSerializer().getFieldForFieldPath(fp, pos + 1);
        }
    }

    @Override
    public FieldType getTypeForFieldPath(S2FieldPath fp, int pos) {
        assert fp.last() >= pos;
        if (fp.last() == pos) {
            return fieldProperties.getType();
        } else {
            return unpackerProperties.getSerializer().getTypeForFieldPath(fp, pos + 1);
        }
    }

    @Override
    public Object getValueForFieldPath(S2FieldPath fp, int pos, ArrayEntityState state) {
        assert fp.last() >= pos;
        int i = fp.get(pos);
        if (fp.last() == pos) {
            return state.has(i);
        } else if (state.isSub(i)) {
            return unpackerProperties.getSerializer().getValueForFieldPath(fp, pos + 1, state.sub(i));
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
            if (state.has(i) && !existing) {
                state.clear(i);
            }
        } else {
            unpackerProperties.getSerializer().setValueForFieldPath(fp, pos + 1, state.sub(i), value);
        }
    }

    @Override
    public S2FieldPath getFieldPathForName(S2ModifiableFieldPath fp, String property) {
        return unpackerProperties.getSerializer().getFieldPathForName(fp, property);
    }

    @Override
    public void collectFieldPaths(S2ModifiableFieldPath fp, List<FieldPath> entries, ArrayEntityState state) {
        int i = fp.cur();
        if (state.has(i)) {
            fp.down();
            unpackerProperties.getSerializer().collectFieldPaths(fp, entries, state.sub(i));
            fp.up(1);
        }
    }

}
