package skadistats.clarity.model.s2.field;

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
        addBasePropertyName(parts);
        if (fp.last >= pos) {
            properties.getSerializer().accumulateName(fp, pos, parts);
        }
    }

    @Override
    public Unpacker getUnpackerForFieldPath(FieldPath fp, int pos) {
        if (fp.last == pos - 1) {
            return baseUnpacker;
        } else {
            return properties.getSerializer().getUnpackerForFieldPath(fp, pos);
        }
    }

    @Override
    public Field getFieldForFieldPath(FieldPath fp, int pos) {
        if (fp.last == pos - 1) {
            return this;
        } else {
            return properties.getSerializer().getFieldForFieldPath(fp, pos);
        }
    }

    @Override
    public FieldType getTypeForFieldPath(FieldPath fp, int pos) {
        if (fp.last == pos - 1) {
            return properties.getType();
        } else {
            return properties.getSerializer().getTypeForFieldPath(fp, pos);
        }
    }

    @Override
    public Object getValueForFieldPath(FieldPath fp, int pos, Object[] state) {
        int i = fp.path[pos - 1];
        Object[] subState = (Object[]) state[i];
        return properties.getSerializer().getValueForFieldPath(fp, pos, subState);
    }

    @Override
    public void setValueForFieldPath(FieldPath fp, int pos, Object[] state, Object value) {
        int i = fp.path[pos - 1];
        Object[] subState = (Object[]) state[i];
        if (fp.last == pos - 1) {
            boolean existing = ((Boolean) value).booleanValue();
            if (subState == null && existing) {
                state[i] = properties.getSerializer().getInitialState();
            } else if (subState != null && !existing) {
                state[i] = null;
            }
        } else {
            properties.getSerializer().setValueForFieldPath(fp, pos, subState, value);
        }
    }

    @Override
    public FieldPath getFieldPathForName(FieldPath fp, String property) {
        return properties.getSerializer().getFieldPathForName(fp, property);
    }

}
