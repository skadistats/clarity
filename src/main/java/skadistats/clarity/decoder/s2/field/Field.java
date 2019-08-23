package skadistats.clarity.decoder.s2.field;

import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.model.s2.S2FieldPath;
import skadistats.clarity.model.s2.S2ModifiableFieldPath;
import skadistats.clarity.model.state.ArrayEntityState;

import java.util.List;

public abstract class Field {

    protected final FieldProperties properties;

    public Field(FieldProperties properties) {
        this.properties = properties;
    }

    public abstract void accumulateName(S2FieldPath fp, int pos, List<String> parts);
    public abstract Unpacker getUnpackerForFieldPath(S2FieldPath fp, int pos);
    public abstract Field getFieldForFieldPath(S2FieldPath fp, int pos);
    public abstract FieldType getTypeForFieldPath(S2FieldPath fp, int pos);
    public abstract Object getValueForFieldPath(S2FieldPath fp, int pos, ArrayEntityState state);
    public abstract void setValueForFieldPath(S2FieldPath fp, int pos, ArrayEntityState state, Object data);
    public abstract S2FieldPath getFieldPathForName(S2ModifiableFieldPath fp, String property);
    public abstract void collectFieldPaths(S2ModifiableFieldPath fp, List<FieldPath> entries, ArrayEntityState state);

    protected void addBasePropertyName(List<String> parts) {
        parts.add(properties.getName());
    }

    public FieldProperties getProperties() {
        return properties;
    }

}
