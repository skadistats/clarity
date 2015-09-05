package skadistats.clarity.model.s2.field;

import skadistats.clarity.decoder.s2.S2UnpackerFactory;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.s2.FieldPath;

import java.util.List;

public class SimpleField extends Field {

    private final Unpacker unpacker;

    public SimpleField(FieldProperties properties) {
        super(properties);
        unpacker = S2UnpackerFactory.createUnpacker(properties, properties.getType().getBaseType());
    }

    @Override
    public Object getInitialState() {
        return null;
    }

    @Override
    public void accumulateName(List<String> parts, FieldPath fp, int pos) {
        assertFieldPathEnd(fp, pos);
        addBasePropertyName(parts);
    }

    @Override
    public Unpacker queryUnpacker(FieldPath fp, int pos) {
        assertFieldPathEnd(fp, pos);
        return unpacker;
    }

    @Override
    public Field queryField(FieldPath fp, int pos) {
        assertFieldPathEnd(fp, pos);
        return this;
    }

    @Override
    public FieldType queryType(FieldPath fp, int pos) {
        assertFieldPathEnd(fp, pos);
        return properties.getType();
    }

    @Override
    public void setValueForFieldPath(FieldPath fp, Object[] state, Object data, int pos) {
        assertFieldPathEnd(fp, pos);
        state[fp.path[pos]] = data;
    }

}
