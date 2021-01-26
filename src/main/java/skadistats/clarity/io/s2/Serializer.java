package skadistats.clarity.io.s2;

public class Serializer {

    private final SerializerId id;
    private final Field[] fields;
    private final String[] fieldNames;

    public Serializer(SerializerId id, Field[] fields, String[] fieldNames) {
        this.id = id;
        this.fields = fields;
        this.fieldNames = fieldNames;
    }

    public SerializerId getId() {
        return id;
    }

    public int getFieldCount() {
        return fields.length;
    }

    public Field getField(int idx) {
        return fields[idx];
    }

    public String getFieldName(int idx) {
        return fieldNames[idx];
    }

    public Integer getFieldIndex(String name) {
        int searchHash = name.hashCode();
        for (int i = 0; i < fields.length; i++) {
            String fieldName = fieldNames[i];
            if (searchHash != fieldName.hashCode()) continue;
            if (name.equals(fieldName)) return i;
        }
        return null;
    }

}
