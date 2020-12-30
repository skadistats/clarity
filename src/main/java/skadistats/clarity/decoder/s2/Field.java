package skadistats.clarity.decoder.s2;

import skadistats.clarity.decoder.s2.field.AccessorFunction;
import skadistats.clarity.decoder.s2.field.FieldProperties;
import skadistats.clarity.decoder.s2.field.FieldType;
import skadistats.clarity.decoder.s2.field.UnpackerProperties;
import skadistats.clarity.decoder.unpacker.Unpacker;

public abstract class Field {

    protected final FieldProperties fieldProperties;
    protected final UnpackerProperties unpackerProperties;

    public Field(FieldProperties fieldProperties, UnpackerProperties unpackerProperties) {
        this.fieldProperties = fieldProperties;
        this.unpackerProperties = unpackerProperties;
    }

    public FieldProperties getFieldProperties() {
        return fieldProperties;
    }

    public UnpackerProperties getUnpackerProperties() {
        return unpackerProperties;
    }

    public String toString() {
        return fieldProperties.getName(0) + "(" + fieldProperties.getType() + ")";
    }

    public abstract AccessorFunction<Field> getFieldAccessor();
    public abstract AccessorFunction<Unpacker<?>> getUnpackerAccessor();
    public abstract AccessorFunction<FieldType> getTypeAccessor();

}
