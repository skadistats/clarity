package skadistats.clarity.model.s2.field;

import skadistats.clarity.decoder.Util;
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
    public void accumulateName(List<String> parts, FieldPath fp, int pos) {
        addBasePropertyName(parts);
        if (fp.last != pos) {
            assertFieldPathEnd(fp, pos + 1);
            parts.add(Util.arrayIdxToString(fp.path[++pos]));
        }
    }

    @Override
    public Unpacker queryUnpacker(FieldPath fp, int pos) {
        if (pos == fp.last) {
            return baseUnpacker;
        } else {
            assertFieldPathEnd(fp, pos + 1);
            return elementUnpacker;
        }
    }

    @Override
    public Field queryField(FieldPath fp, int pos) {
        if (pos == fp.last) {
            return this;
        } else {
            assertFieldPathEnd(fp, pos + 1);
            return this;
        }
    }

    @Override
    public FieldType queryType(FieldPath fp, int pos) {
        if (pos == fp.last) {
            return properties.getType();
        } else {
            assertFieldPathEnd(fp, pos + 1);
            return properties.getType().getGenericType();
        }
    }

    @Override
    public void setValueForFieldPath(FieldPath fp, Object[] state, Object data, int pos) {
        int i = fp.path[pos];
        Object[] myState = (Object[]) state[i];
        if (pos == fp.last) {
            int size = ((Integer) data).intValue();
            int curSize = myState == null ? 0 : myState.length;
            if (myState == null && size > 0) {
                state[i] = new Object[size];
            } else if (myState != null && size == 0) {
                state[i] = null;
            } else if (curSize != size) {
                state[i] = new Object[size];
                System.arraycopy(myState, 0, state[i], 0, Math.min(myState.length, size));
            }
        } else {
            assertFieldPathEnd(fp, pos + 1);
            myState[fp.path[pos + 1]] = data;
        }
    }

    @Override
    public Object getValueForFieldPath(FieldPath fp, Object[] state, int pos) {
        assertFieldPathEnd(fp, pos + 1);
        Object[] myState = (Object[]) state[fp.path[pos]];
        return myState[fp.path[pos + 1]];
    }
    
}
