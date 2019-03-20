package skadistats.clarity.decoder.s2;

import skadistats.clarity.decoder.s2.field.Field;
import skadistats.clarity.decoder.s2.field.FieldType;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.CloneableEntityState;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.model.state.EntityStateFactory;
import skadistats.clarity.util.TextTable;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class S2DTClass implements DTClass {

    private final Serializer serializer;
    private int classId = -1;

    public S2DTClass(Serializer serializer) {
        this.serializer = serializer;
    }

    @Override
    public int getClassId() {
        return classId;
    }

    @Override
    public void setClassId(int classId) {
        this.classId = classId;
    }

    @Override
    public String getDtName() {
        return serializer.getId().getName();
    }

    public Serializer getSerializer() {
        return serializer;
    }

    @Override
    public CloneableEntityState getEmptyStateArray() {
        CloneableEntityState state = EntityStateFactory.withLength(serializer.getFieldCount());
        serializer.initInitialState(state);
        return state;
    }

    public String getNameForFieldPath(FieldPath fp) {
        List<String> parts = new ArrayList<>();
        serializer.accumulateName(fp, 0, parts);
        StringBuilder b = new StringBuilder();
        for (String part : parts) {
            if (b.length() != 0) {
                b.append('.');
            }
            b.append(part);
        }
        return b.toString();
    }

    public Unpacker getUnpackerForFieldPath(FieldPath fp) {
        return serializer.getUnpackerForFieldPath(fp, 0);
    }

    public Field getFieldForFieldPath(FieldPath fp) {
        return serializer.getFieldForFieldPath(fp, 0);
    }

    public FieldType getTypeForFieldPath(FieldPath fp) {
        return serializer.getTypeForFieldPath(fp, 0);
    }

    @Override
    public <T> T getValueForFieldPath(FieldPath fp, EntityState state) {
        return (T) serializer.getValueForFieldPath(fp, 0, state);
    }

    public void setValueForFieldPath(FieldPath fp, EntityState state, Object value) {
        serializer.setValueForFieldPath(fp, 0, state, value);
    }

    @Override
    public FieldPath getFieldPathForName(String property) {
        FieldPath fp = new FieldPath();
        return serializer.getFieldPathForName(fp, property);
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", serializer.getId(), classId);
    }


    private static final ReentrantLock DEBUG_LOCK = new ReentrantLock();
    private static final TextTable DEBUG_DUMPER = new TextTable.Builder()
        .setFrame(TextTable.FRAME_COMPAT)
        .addColumn("FP")
        .addColumn("Property")
        .addColumn("Value")
        .build();

    @Override
    public String dumpState(String title, EntityState state) {
        FieldPath fp = new FieldPath();
        List<DumpEntry> entries = new ArrayList<>();
        serializer.collectDump(fp, "", entries, state);
        DEBUG_LOCK.lock();
        try {
            DEBUG_DUMPER.clear();
            DEBUG_DUMPER.setTitle(title);
            int r = 0;
            for (DumpEntry entry : entries) {
                DEBUG_DUMPER.setData(r, 0, entry.fieldPath);
                DEBUG_DUMPER.setData(r, 1, entry.name);
                DEBUG_DUMPER.setData(r, 2, entry.value);
                r++;
            }
            return DEBUG_DUMPER.toString();
        } finally {
            DEBUG_LOCK.unlock();
        }
    }

    @Override
    public List<FieldPath> collectFieldPaths(EntityState state) {
        List<FieldPath> result = new ArrayList<>(state.length());
        serializer.collectFieldPaths(new FieldPath(), result, state);
        return result;
    }

}
