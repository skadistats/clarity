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
    public FieldPath getFieldPathForName(String property) {
        FieldPath fp = new FieldPath();
        return serializer.getFieldPathForName(fp, property);
    }

    @Override
    public <T> T getValueForFieldPath(Object[] state, FieldPath fp) {
        return (T) serializer.getFields()[fp.path[0]].getValueForFieldPath(fp, state, 0);
    }

    @Override
    public Object[] getEmptyStateArray() {
        return serializer.getInitialState();
    }

    public String getNameForFieldPath(FieldPath fp) {
        List<String> parts = new ArrayList<>();
        serializer.getFields()[fp.path[0]].accumulateName(parts, fp, 0);
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
        return serializer.getFields()[fp.path[0]].queryUnpacker(fp, 0);
    }

    public Field getFieldForFieldPath(FieldPath fp) {
        return serializer.getFields()[fp.path[0]].queryField(fp, 0);
    }

    public FieldType getTypeForFieldPath(FieldPath fp) {
        return serializer.getFields()[fp.path[0]].queryType(fp, 0);
    }

    public void setValueForFieldPath(FieldPath fp, Object[] state, Object data) {
        serializer.getFields()[fp.path[0]].setValueForFieldPath(fp, state, data, 0);
    }

}
