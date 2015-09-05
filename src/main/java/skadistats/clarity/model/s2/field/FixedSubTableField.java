package skadistats.clarity.model.s2.field;

import skadistats.clarity.decoder.s2.S2UnpackerFactory;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.s2.FieldPath;

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
    public void accumulateName(List<String> parts, FieldPath fp, int pos) {
        addBasePropertyName(parts);
        if (fp.last != pos) {
            pos++;
            properties.getSerializer().getFields()[fp.path[pos]].accumulateName(parts, fp, pos);
        }
    }

    @Override
    public Unpacker queryUnpacker(FieldPath fp, int pos) {
        if (pos == fp.last) {
            return baseUnpacker;
        } else {
            pos++;
            return properties.getSerializer().getFields()[fp.path[pos]].queryUnpacker(fp, pos);
        }
    }

    @Override
    public Field queryField(FieldPath fp, int pos) {
        if (pos == fp.last) {
            return this;
        } else {
            pos++;
            return properties.getSerializer().getFields()[fp.path[pos]].queryField(fp, pos);
        }
    }

    @Override
    public FieldType queryType(FieldPath fp, int pos) {
        if (pos == fp.last) {
            return properties.getType();
        } else {
            pos++;
            return properties.getSerializer().getFields()[fp.path[pos]].queryType(fp, pos);
        }
    }

    @Override
    public void setValueForFieldPath(FieldPath fp, Object[] state, Object data, int pos) {
        int i = fp.path[pos];
        Object[] myState = (Object[]) state[i];
        if (pos == fp.last) {
            boolean amThere = ((Boolean) data).booleanValue();
            if (myState == null && amThere) {
                state[i] = properties.getSerializer().getInitialState();
            } else if (myState != null && !amThere) {
                state[i] = null;
            }
        } else {
            properties.getSerializer().getFields()[fp.path[pos + 1]].setValueForFieldPath(fp, myState, data, pos + 1);
        }
    }
}
