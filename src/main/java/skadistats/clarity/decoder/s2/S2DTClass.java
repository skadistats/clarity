package skadistats.clarity.decoder.s2;

import skadistats.clarity.decoder.s2.field.Field;
import skadistats.clarity.decoder.s2.field.FieldType;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.model.state.EntityStateFactory;

import java.util.ArrayList;
import java.util.List;

public class S2DTClass implements DTClass<S2FieldPath> {

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
    public EntityState<S2FieldPath> getEmptyState() {
        return EntityStateFactory.forS2(serializer);
    }

    @Override
    public String getNameForFieldPath(S2FieldPath fp) {
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

    public Unpacker getUnpackerForFieldPath(S2FieldPath fp) {
        return serializer.getUnpackerForFieldPath(fp, 0);
    }

    public Field getFieldForFieldPath(S2FieldPath fp) {
        return serializer.getFieldForFieldPath(fp, 0);
    }

    public FieldType getTypeForFieldPath(S2FieldPath fp) {
        return serializer.getTypeForFieldPath(fp, 0);
    }

    @Override
    public S2FieldPath getFieldPathForName(String property) {
        S2FieldPath fp = S2FieldPath.createEmpty();
        return serializer.getFieldPathForName(fp, property);
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", serializer.getId(), classId);
    }

}
