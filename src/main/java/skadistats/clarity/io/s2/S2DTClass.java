package skadistats.clarity.io.s2;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.s2.field.SerializerField;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.S2ModifiableFieldPath;
import skadistats.clarity.model.state.EntityState;
import skadistats.clarity.model.state.EntityStateFactory;

public class S2DTClass implements DTClass {

    private final SerializerField field;
    private int classId = -1;

    public S2DTClass(SerializerField field) {
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

    public Field getFieldForFieldPath(S2FieldPath fp) {
            switch (fp.last()) {
            case 0: return field.getChild(fp.get(0));
            case 1: return field.getChild(fp.get(0)).getChild(fp.get(1));
            case 2: return field.getChild(fp.get(0)).getChild(fp.get(1)).getChild(fp.get(2));
            case 3: return field.getChild(fp.get(0)).getChild(fp.get(1)).getChild(fp.get(2)).getChild(fp.get(3));
            case 4: return field.getChild(fp.get(0)).getChild(fp.get(1)).getChild(fp.get(2)).getChild(fp.get(3)).getChild(fp.get(4));
            case 5: return field.getChild(fp.get(0)).getChild(fp.get(1)).getChild(fp.get(2)).getChild(fp.get(3)).getChild(fp.get(4)).getChild(fp.get(5));
            case 6: return field.getChild(fp.get(0)).getChild(fp.get(1)).getChild(fp.get(2)).getChild(fp.get(3)).getChild(fp.get(4)).getChild(fp.get(5)).getChild(fp.get(6));
            default: throw new UnsupportedOperationException();
        }
    }

    public Decoder getDecoderForFieldPath(S2FieldPath fp) {
        var f = getFieldForFieldPath(fp);
        return f != null ? f.getDecoder() : null;
    }

    public FieldType getTypeForFieldPath(S2FieldPath fp) {
        var f = getFieldForFieldPath(fp);
        return f != null ? f.getType() : null;
    }

    @Override
    public String getNameForFieldPath(FieldPath fpX) {
        var fp = fpX.s2();
        var sb = new StringBuilder();

        Field currentField = field;
        var i = 0;
        var last = fp.last();
        while(true) {
            var idx = fp.get(i);
            var segment = currentField.getChildNameSegment(idx);
            if (segment == null) return null;
            if (i != 0) sb.append('.');
            sb.append(segment);
            if (i == last) return sb.toString();
            currentField = currentField.getChild(idx);
            i++;
        }
    }

    @Override
    public S2FieldPath getFieldPathForName(String fieldName) {
        var fp = S2ModifiableFieldPath.newInstance();

        Field currentField = field;
        var search = fieldName;
        while(true) {
            var dotIdx = search.indexOf('.');
            var last = (dotIdx == -1);
            var segment = last ? search : search.substring(0, dotIdx);
            var fieldIdx = currentField.getChildIndex(segment);
            if (fieldIdx == null) return null;
            fp.cur(fieldIdx);
            if (last) return fp.unmodifiable();
            fp.down();
            currentField = currentField.getChild(fieldIdx);
            search = search.substring(segment.length() + 1);
        }
    }

    @Override
    public String toString() {
        return String.format("%s (%s)", field.getSerializer().getId(), classId);
    }

}
