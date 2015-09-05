package skadistats.clarity.model.s2.field;

import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.FieldPath;

import java.util.List;

public abstract class Field {

    protected final FieldProperties properties;

    public Field(FieldProperties properties) {
        this.properties = properties;
    }

    public abstract Object getInitialState();
    public abstract void accumulateName(List<String> parts, FieldPath fp, int pos);
    public abstract Unpacker queryUnpacker(FieldPath fp, int pos);
    public abstract Field queryField(FieldPath fp, int pos);
    public abstract FieldType queryType(FieldPath fp, int pos);
    public abstract void setValueForFieldPath(FieldPath fp, Object[] state, Object data, int pos);

    protected void assertFieldPathEnd(FieldPath fp, int pos) {
        if (fp.last != pos) {
            throw new RuntimeException(String.format("Assert failed: FieldPath %s not at end at position %s", fp, pos));
        }
    }

    protected void addBasePropertyName(List<String> parts) {
        if (properties.getSendNode() != null) {
            parts.add(properties.getSendNode());
        }
        parts.add(properties.getName());
    }


    public FieldProperties getProperties() {
        return properties;
    }

}
