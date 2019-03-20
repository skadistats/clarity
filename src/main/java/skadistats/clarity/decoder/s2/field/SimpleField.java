package skadistats.clarity.decoder.s2.field;

import skadistats.clarity.decoder.s2.DumpEntry;
import skadistats.clarity.decoder.s2.S2UnpackerFactory;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.EntityState;

import java.util.List;

public class SimpleField extends Field {

    private final Unpacker unpacker;

    public SimpleField(FieldProperties properties) {
        super(properties);
        unpacker = S2UnpackerFactory.createUnpacker(properties, properties.getType().getBaseType());
    }

    @Override
    public void initInitialState(EntityState state, int idx) {
        state.set(idx, null);
    }

    @Override
    public void accumulateName(FieldPath fp, int pos, List<String> parts) {
        assert fp.last == pos;
        addBasePropertyName(parts);
    }

    @Override
    public Unpacker getUnpackerForFieldPath(FieldPath fp, int pos) {
        assert fp.last == pos;
        return unpacker;
    }

    @Override
    public Field getFieldForFieldPath(FieldPath fp, int pos) {
        assert fp.last == pos;
        return this;
    }

    @Override
    public FieldType getTypeForFieldPath(FieldPath fp, int pos) {
        assert fp.last == pos;
        return properties.getType();
    }

    @Override
    public Object getValueForFieldPath(FieldPath fp, int pos, EntityState state) {
        assert fp.last == pos;
        return state.get(fp.path[pos]);
    }

    @Override
    public void setValueForFieldPath(FieldPath fp, int pos, EntityState state, Object value) {
        assert fp.last == pos;
        state.set(fp.path[pos], value);
    }

    @Override
    public FieldPath getFieldPathForName(FieldPath fp, String name) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void collectDump(FieldPath fp, String namePrefix, List<DumpEntry> entries, EntityState state) {
        entries.add(new DumpEntry(fp, joinPropertyName(namePrefix, properties.getName()), state.get(fp.path[fp.last])));
    }

    @Override
    public void collectFieldPaths(FieldPath fp, List<FieldPath> entries, EntityState state) {
        entries.add(new FieldPath(fp));
    }
}
