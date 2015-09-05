package skadistats.clarity.model.s2;

import skadistats.clarity.model.s2.field.Field;

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

    public Object[] getInitialState() {
        Object[] result = new Object[fields.length];
        for (int i = 0; i < fields.length; i++) {
            result[i] = fields[i].getInitialState();
        }
        return result;
    }

}
