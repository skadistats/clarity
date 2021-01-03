package skadistats.clarity.decoder.s2;

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

    public int getFieldCount() {
        return fields.length;
    }

    public Field getField(int idx) {
        return fields[idx];
    }

    public Integer getFieldIndex(String name) {
        int searchHash = name.hashCode();
        for (int i = 0; i < fields.length; i++) {
            String fieldName = fields[i].getFieldProperties().getName();
            if (searchHash != fieldName.hashCode()) continue;
            if (name.equals(fieldName)) return i;
        }
        return null;
    }

}
