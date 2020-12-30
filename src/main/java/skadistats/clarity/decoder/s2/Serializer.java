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

    public Field getField(int i) {
        return fields[i];
    }

}
