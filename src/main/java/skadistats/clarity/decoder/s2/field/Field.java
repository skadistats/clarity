package skadistats.clarity.decoder.s2.field;

import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.state.ArrayEntityState;

import java.util.List;

public abstract class Field {

    protected final FieldProperties properties;

    public Field(FieldProperties properties) {
        this.properties = properties;
    }

    public abstract void initInitialState(ArrayEntityState state, int idx);
    public abstract void accumulateName(FieldPath fp, int pos, List<String> parts);
    public abstract Unpacker getUnpackerForFieldPath(FieldPath fp, int pos);
    public abstract Field getFieldForFieldPath(FieldPath fp, int pos);
    public abstract FieldType getTypeForFieldPath(FieldPath fp, int pos);
    public abstract Object getValueForFieldPath(FieldPath fp, int pos, ArrayEntityState state);
    public abstract void setValueForFieldPath(FieldPath fp, int pos, ArrayEntityState state, Object data);
    public abstract FieldPath getFieldPathForName(FieldPath fp, String property);
    public abstract void collectFieldPaths(FieldPath fp, List<FieldPath> entries, ArrayEntityState state);

    protected void addBasePropertyName(List<String> parts) {
        parts.add(properties.getName());
    }

    public FieldProperties getProperties() {
        return properties;
    }

}
