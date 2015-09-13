package skadistats.clarity.model.s2;

import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.field.Field;
import skadistats.clarity.model.s2.field.FieldType;

import java.util.ArrayList;
import java.util.List;

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

    @Override
    public Object[] getEmptyStateArray() {
        return serializer.getInitialState();
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
    public <T> T getValueForFieldPath(FieldPath fp, Object[] state) {
        return (T) serializer.getValueForFieldPath(fp, 0, state);
    }

    public void setValueForFieldPath(FieldPath fp, Object[] state, Object value) {
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

}
