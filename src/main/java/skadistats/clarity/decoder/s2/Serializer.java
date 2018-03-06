package skadistats.clarity.decoder.s2;

import skadistats.clarity.decoder.s2.field.Field;
import skadistats.clarity.decoder.s2.field.FieldType;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.FieldPath;

import java.util.Comparator;
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

        sendNodePrefixes = new TreeSet<>(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                int result = Integer.compare(o2.length(), o1.length());
                return result != 0 ? result : o1.compareTo(o2);
            }
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

    public Object[] getInitialState() {
        Object[] result = new Object[fields.length];
        for (int i = 0; i < fields.length; i++) {
            result[i] = fields[i].getInitialState();
        }
        return result;
    }

    public void accumulateName(FieldPath fp, int pos, List<String> parts) {
        fields[fp.path[pos]].accumulateName(fp, pos, parts);
    }

    public Unpacker getUnpackerForFieldPath(FieldPath fp, int pos) {
        return fields[fp.path[pos]].getUnpackerForFieldPath(fp, pos);
    }

    public Field getFieldForFieldPath(FieldPath fp, int pos) {
        return fields[fp.path[pos]].getFieldForFieldPath(fp, pos);
    }

    public FieldType getTypeForFieldPath(FieldPath fp, int pos) {
        return fields[fp.path[pos]].getTypeForFieldPath(fp, pos);
    }

    public Object getValueForFieldPath(FieldPath fp, int pos, Object[] state) {
        return fields[fp.path[pos]].getValueForFieldPath(fp, pos, state);
    }

    public void setValueForFieldPath(FieldPath fp, int pos, Object[] state, Object data) {
        fields[fp.path[pos]].setValueForFieldPath(fp, pos, state, data);
    }

    private FieldPath getFieldPathForNameInternal(FieldPath fp, String property) {
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            String fieldName = field.getProperties().getName();
            if (property.startsWith(fieldName)) {
                fp.path[fp.last] = i;
                if (property.length() == fieldName.length()) {
                    return fp;
                } else {
                    if (property.charAt(fieldName.length()) != '.') {
                        continue;
                    }
                    property = property.substring(fieldName.length() + 1);
                    fp.last++;
                    return field.getFieldPathForName(fp, property);
                }
            }
        }
        return null;
    }

    public FieldPath getFieldPathForName(FieldPath fp, String property) {
//        for (String sendNodePrefix : sendNodePrefixes) {
//            if (property.length() > sendNodePrefix.length() && property.startsWith(sendNodePrefix)) {
//                return getFieldPathForNameInternal(fp, property.substring(sendNodePrefix.length() + 1));
//            }
//        }
        return getFieldPathForNameInternal(fp, property);
    }

    public void collectDump(FieldPath fp, String namePrefix, List<DumpEntry> entries, Object[] state) {
        for (int i = 0; i < fields.length; i++) {
            if (state[i] != null) {
                fp.path[fp.last] = i;
                fields[i].collectDump(fp, namePrefix, entries, state);
            }
        }
    }

    public void collectFieldPaths(FieldPath fp, List<FieldPath> entries, Object[] state) {
        for (int i = 0; i < fields.length; i++) {
            if (state[i] != null) {
                fp.path[fp.last] = i;
                fields[i].collectFieldPaths(fp, entries, state);
            }
        }
    }


}
