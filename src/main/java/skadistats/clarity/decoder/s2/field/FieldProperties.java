package skadistats.clarity.decoder.s2.field;

import skadistats.clarity.decoder.s2.field.FieldType;

public interface FieldProperties {

    FieldType getType();
    String getName(int i);

}
