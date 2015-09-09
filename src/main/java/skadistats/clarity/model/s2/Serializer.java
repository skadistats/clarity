package skadistats.clarity.model.s2;

import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.field.Field;

import java.util.Comparator;
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

    private FieldPath getFieldPathForNameInternal(FieldPath fp, String property) {
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            String fieldName = field.getProperties().getName();
            if (property.startsWith(fieldName)) {
                fp.path[fp.last] = i;
                if (property.length() == fieldName.length()) {
                    return fp;
                } else {
                    property = property.substring(fieldName.length());
                    if (property.charAt(0) != '.') {
                        throw new RuntimeException("unresolvable fieldpath");
                    }
                    property = property.substring(1);
                    fp.last++;
                    return field.getFieldPathForName(fp, property);
                }
            }
        }
        return null;
    }

    public FieldPath getFieldPathForName(FieldPath fp, String property) {
        for (String sendNodePrefix : sendNodePrefixes) {
            if (property.length() > sendNodePrefix.length() && property.startsWith(sendNodePrefix)) {
                return getFieldPathForNameInternal(fp, property.substring(sendNodePrefix.length() + 1));
            }
        }
        return getFieldPathForNameInternal(fp, property);
    }

}
