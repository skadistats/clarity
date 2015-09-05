package skadistats.clarity.model.s2.field;

import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.s2.S2UnpackerFactory;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.s2.FieldPath;

import java.util.List;

public class VarSubTableField extends Field {

    private final Unpacker baseUnpacker;

    public VarSubTableField(FieldProperties properties) {
        super(properties);
        baseUnpacker = S2UnpackerFactory.createUnpacker(properties, "uint32");
    }

    @Override
    public Object getInitialState() {
        return new Object[] { null, null };
    }

    @Override
    public void accumulateName(List<String> parts, FieldPath fp, int pos) {
        addBasePropertyName(parts);
        if (fp.last != pos) {
            pos++;
            parts.add(Util.arrayIdxToString(fp.path[pos]));
            if (fp.last != pos) {
                pos++;
                properties.getSerializer().getFields()[fp.path[pos]].accumulateName(parts, fp, pos);
            }
        }
    }

    @Override
    public Unpacker queryUnpacker(FieldPath fp, int pos) {
        if (pos == fp.last) {
            return baseUnpacker;
        }
        pos++;
        if (pos == fp.last) {
            return baseUnpacker;
        }
        pos++;
        return properties.getSerializer().getFields()[fp.path[pos]].queryUnpacker(fp, pos);
    }

    @Override
    public Field queryField(FieldPath fp, int pos) {
        if (pos == fp.last) {
            return this;
        }
        pos++;
        if (pos == fp.last) {
            return this;
        }
        pos++;
        return properties.getSerializer().getFields()[fp.path[pos]].queryField(fp, pos);
    }

    @Override
    public FieldType queryType(FieldPath fp, int pos) {
        if (pos == fp.last) {
            return properties.getType();
        }
        pos++;
        if (pos == fp.last) {
            return properties.getType();
        }
        pos++;
        return properties.getSerializer().getFields()[fp.path[pos]].queryType(fp, pos);
    }

}
