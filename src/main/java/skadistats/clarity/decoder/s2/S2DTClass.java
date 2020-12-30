package skadistats.clarity.decoder.s2;

import skadistats.clarity.decoder.s2.field.AccessorFunction;
import skadistats.clarity.decoder.s2.field.FieldNameAccumulator;
import skadistats.clarity.decoder.s2.field.FieldType;
import skadistats.clarity.decoder.s2.field.impl.RecordField;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.S2ModifiableFieldPath;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.model.state.EntityStateFactory;

import static skadistats.clarity.decoder.s2.field.AccessorFunction.performAccess;

public class S2DTClass implements DTClass {

    private final RecordField field;
    private int classId = -1;

    public S2DTClass(RecordField field) {
        this.field = field;
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
        return field.getSerializer().getId().getName();
    }

    public Serializer getSerializer() {
        return field.getSerializer();
    }

    @Override
    public EntityState getEmptyState() {
        return EntityStateFactory.forS2(field);
    }

    @Override
    public String getNameForFieldPath(FieldPath fpX) {
        return performAccess(
                new FieldNameAccumulator(field.getFieldAccessor()),
                fpX.s2()
        );
    }

    public Unpacker getUnpackerForFieldPath(S2FieldPath fp) {
        return performAccess(field.getUnpackerAccessor(), fp);
    }

    public Field getFieldForFieldPath(S2FieldPath fp) {
        return performAccess(field.getFieldAccessor(), fp);
    }

    public FieldType getTypeForFieldPath(S2FieldPath fp) {
        return performAccess(field.getTypeAccessor(), fp);
    }

    @Override
    public S2FieldPath getFieldPathForName(String fieldName) {
        String search = fieldName;
        S2ModifiableFieldPath fp = S2ModifiableFieldPath.newInstance();
        AccessorFunction<Field> fieldAccessor = field.getFieldAccessor();
        while(true) {
            Field currentField = fieldAccessor.get();
            int dotIdx = search.indexOf('.');
            boolean last = (dotIdx == -1);
            String segment = last ? search : search.substring(0, dotIdx);
            Integer fieldIdx = currentField.getFieldIndex(segment);
            if (fieldIdx == null) {
                throw new IllegalArgumentException(fieldName);
            }
            fp.cur(fieldIdx);
            if (last) break;
            fp.down();
            fieldAccessor = fieldAccessor.down(fieldIdx);
            search = search.substring(segment.length() + 1);
        }
        return fp.unmodifiable();
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", field.getSerializer().getId(), classId);
    }

}
