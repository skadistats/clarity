package skadistats.clarity.decoder.s2;

import skadistats.clarity.decoder.s2.field.Field;
import skadistats.clarity.decoder.s2.field.FieldType;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.S2ModifiableFieldPath;
import skadistats.clarity.model.state.ArrayEntityState;

import java.util.List;

public class Serializer {

    private final SerializerId id;
    private final Field[] fields;

    public Serializer(SerializerId id, Field[] fields) {
        this.id = id;
        this.fields = fields;
    }

    public SerializerId getId() {
        return id;
    }

    public Field[] getFields() {
        return fields;
    }

    public int getFieldCount() {
        return fields.length;
    }

    public void accumulateName(S2FieldPath fp, int pos, List<String> parts) {
        fields[fp.get(pos)].accumulateName(fp, pos, parts);
    }

    public Unpacker getUnpackerForFieldPath(S2FieldPath fp, int pos) {
        return fields[fp.get(pos)].getUnpackerForFieldPath(fp, pos);
    }

    public Field getFieldForFieldPath(S2FieldPath fp, int pos) {
        return fields[fp.get(pos)].getFieldForFieldPath(fp, pos);
    }

    public FieldType getTypeForFieldPath(S2FieldPath fp, int pos) {
        return fields[fp.get(pos)].getTypeForFieldPath(fp, pos);
    }

    public Object getValueForFieldPath(S2FieldPath fp, int pos, ArrayEntityState state) {
        return fields[fp.get(pos)].getValueForFieldPath(fp, pos, state);
    }

    public void setValueForFieldPath(S2FieldPath fp, int pos, ArrayEntityState state, Object data) {
        int idx = fp.get(pos);
        if (!state.has(idx)) {
            state.capacity(fields.length);
        }
        fields[idx].setValueForFieldPath(fp, pos, state, data);
    }

    private S2FieldPath getFieldPathForNameInternal(S2ModifiableFieldPath fp, String property) {
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            String fieldName = field.getFieldProperties().getName();
            if (property.startsWith(fieldName)) {
                fp.cur(i);
                if (property.length() == fieldName.length()) {
                    return fp.unmodifiable();
                } else {
                    if (property.charAt(fieldName.length()) != '.') {
                        continue;
                    }
                    property = property.substring(fieldName.length() + 1);
                    fp.down();
                    return field.getFieldPathForName(fp, property);
                }
            }
        }
        return null;
    }

    public S2FieldPath getFieldPathForName(S2ModifiableFieldPath fp, String property) {
        return getFieldPathForNameInternal(fp, property);
    }

    public void collectFieldPaths(S2ModifiableFieldPath fp, List<FieldPath> entries, ArrayEntityState state) {
        for (int i = 0; i < fields.length; i++) {
            if (state.has(i)) {
                fp.cur(i);
                fields[i].collectFieldPaths(fp, entries, state);
            }
        }
    }


}
