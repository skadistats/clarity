package skadistats.clarity.decoder.s2.field;

import skadistats.clarity.decoder.s2.DumpEntry;
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
    public abstract void collectDump(FieldPath fp, String namePrefix, List<DumpEntry> entries, Object[] state);
    public abstract void collectFieldPaths(FieldPath fp, List<FieldPath> entries, Object[] state);

    protected void addBasePropertyName(List<String> parts) {
        parts.add(properties.getName());
    }

    protected String joinPropertyName(String... parts) {
        StringBuilder b = new StringBuilder();
        for (String part : parts) {
            if (b.length() != 0) {
                b.append('.');
            }
            b.append(part);
        }
        return b.toString();
    }

    public FieldProperties getProperties() {
        return properties;
    }

}
