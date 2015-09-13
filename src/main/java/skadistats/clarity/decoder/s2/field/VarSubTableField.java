package skadistats.clarity.decoder.s2.field;

import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.s2.DumpEntry;
import skadistats.clarity.decoder.s2.S2UnpackerFactory;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.FieldPath;

import java.util.ArrayList;
import java.util.List;

public class VarSubTableField extends Field {

    private final Unpacker baseUnpacker;

    public VarSubTableField(FieldProperties properties) {
        super(properties);
        baseUnpacker = S2UnpackerFactory.createUnpacker(properties, "uint32");
    }

    @Override
    public Object getInitialState() {
        return new ArrayList<>();
    }

    @Override
    public void accumulateName(FieldPath fp, int pos, List<String> parts) {
        addBasePropertyName(parts);
        if (fp.last != pos - 1) {
            parts.add(Util.arrayIdxToString(fp.path[pos]));
            if (fp.last != pos) {
                properties.getSerializer().accumulateName(fp, pos + 1, parts);
            }
        }
    }

    @Override
    public Unpacker getUnpackerForFieldPath(FieldPath fp, int pos) {
        if (fp.last >= pos + 1) {
            return properties.getSerializer().getUnpackerForFieldPath(fp, pos + 1);
        } else {
            return baseUnpacker;
        }
    }

    @Override
    public Field getFieldForFieldPath(FieldPath fp, int pos) {
        if (fp.last >= pos + 1) {
            return properties.getSerializer().getFieldForFieldPath(fp, pos + 1);
        } else {
            return this;
        }
    }

    @Override
    public FieldType getTypeForFieldPath(FieldPath fp, int pos) {
        if (fp.last >= pos + 1) {
            return properties.getSerializer().getTypeForFieldPath(fp, pos + 1);
        } else {
            return properties.getType();
        }
    }

    @Override
    public Object getValueForFieldPath(FieldPath fp, int pos, Object[] state) {
        assert fp.last >= pos + 1;
        List<Object[]> subState = (List<Object[]>) state[fp.path[pos - 1]];
        return properties.getSerializer().getValueForFieldPath(fp, pos + 1, subState.get(fp.path[pos]));
    }

    private void ensureLength(List<Object[]> state, int wanted, boolean shorten) {
        int cur = state.size();
        if (shorten) {
            while (cur > wanted) {
                state.remove(--cur);
            }
        }
        while (cur < wanted) {
            state.add(properties.getSerializer().getInitialState());
            cur++;
        }
    }

    @Override
    public void setValueForFieldPath(FieldPath fp, int pos, Object[] state, Object value) {
        List<Object[]> subState = (List<Object[]>) state[fp.path[pos - 1]];
        int j = fp.path[pos];
        if (fp.last >= pos + 1) {
            ensureLength(subState, j + 1, false);
            properties.getSerializer().setValueForFieldPath(fp, pos + 1, subState.get(j), value);
        } else {
            ensureLength(subState, ((Integer) value).intValue(), true);
        }
    }

    @Override
    public FieldPath getFieldPathForName(FieldPath fp, String property) {
        String idx = property.substring(0, 4);
        fp.path[fp.last] = Integer.valueOf(idx);
        fp.last++;
        return properties.getSerializer().getFieldPathForName(fp, property.substring(5));
    }

    @Override
    public void collectDump(FieldPath fp, String namePrefix, List<DumpEntry> entries, Object[] state) {
        List<Object[]> subState = (List<Object[]>) state[fp.path[fp.last]];
        String name = joinPropertyName(namePrefix, properties.getName());
        if (subState.size() > 0) {
            entries.add(new DumpEntry(fp, name, subState.size()));
            fp.last += 2;
            for (int i = 0; i < subState.size(); i++) {
                fp.path[fp.last - 1] = i;
                properties.getSerializer().collectDump(fp, joinPropertyName(name, Util.arrayIdxToString(i)), entries, subState.get(i));
            }
            fp.last -= 2;
        }
    }

}
