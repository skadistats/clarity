package skadistats.clarity.decoder.s2;

import skadistats.clarity.decoder.s2.field.Field;
import skadistats.clarity.decoder.s2.field.FieldType;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.state.ArrayEntityState;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Serializer {

    private final SerializerId id;
    private final Field[] fields;
    private final Set<String> sendNodePrefixes;

    public Serializer(SerializerId id, Field[] fields) {
        this.id = id;
        this.fields = fields;

        sendNodePrefixes = new TreeSet<>((o1, o2) -> {
            int result = Integer.compare(o2.length(), o1.length());
            return result != 0 ? result : o1.compareTo(o2);
        });
        for (Field field : fields) {
            if (field.getProperties().getSendNode() != null) {
                sendNodePrefixes.add(field.getProperties().getSendNode());
            }
        }
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

    public void initInitialState(ArrayEntityState state) {
        state.capacity(fields.length);
        for (int i = 0; i < fields.length; i++) {
            fields[i].initInitialState(state, i);
        }
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
        fields[fp.get(pos)].setValueForFieldPath(fp, pos, state, data);
    }

    private S2FieldPath getFieldPathForNameInternal(S2FieldPath fp, String property) {
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            String fieldName = field.getProperties().getName();
            if (property.startsWith(fieldName)) {
                fp.cur(i);
                if (property.length() == fieldName.length()) {
                    return fp;
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

    public S2FieldPath getFieldPathForName(S2FieldPath fp, String property) {
//        for (String sendNodePrefix : sendNodePrefixes) {
//            if (property.length() > sendNodePrefix.length() && property.startsWith(sendNodePrefix)) {
//                return getFieldPathForNameInternal(fp, property.substring(sendNodePrefix.length() + 1));
//            }
//        }
        return getFieldPathForNameInternal(fp, property);
    }

    public void collectFieldPaths(S2FieldPath fp, List<FieldPath> entries, ArrayEntityState state) {
        for (int i = 0; i < fields.length; i++) {
            if (state.has(i)) {
                fp.cur(i);
                fields[i].collectFieldPaths(fp, entries, state);
            }
        }
    }


}
