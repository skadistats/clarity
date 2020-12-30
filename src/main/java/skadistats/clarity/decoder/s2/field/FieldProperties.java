package skadistats.clarity.decoder.s2.field;

import java.util.function.IntFunction;

public class FieldProperties {

    private FieldType fieldType;
    private IntFunction<String> nameFunction;

    public FieldProperties(FieldType fieldType, IntFunction<String> nameFunction) {
        this.fieldType = fieldType;
        this.nameFunction = nameFunction;
    }

    public FieldType getType() {
        return fieldType;
    }

    public String getName(int i) {
        return nameFunction.apply(i);
    }

}
