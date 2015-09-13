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
    public abstract void accumulateName(FieldPath fp, int pos, List<String> parts);
    public abstract Unpacker getUnpackerForFieldPath(FieldPath fp, int pos);
    public abstract Field getFieldForFieldPath(FieldPath fp, int pos);
    public abstract FieldType getTypeForFieldPath(FieldPath fp, int pos);
    public abstract Object getValueForFieldPath(FieldPath fp, int pos, Object[] state);
    public abstract void setValueForFieldPath(FieldPath fp, int pos, Object[] state, Object data);
    public abstract FieldPath getFieldPathForName(FieldPath fp, String property);

    protected void addBasePropertyName(List<String> parts) {
//        if (properties.getSendNode() != null) {
//            parts.add(properties.getSendNode());
//        }
        parts.add(properties.getName());
    }

    public FieldProperties getProperties() {
        return properties;
    }

}
