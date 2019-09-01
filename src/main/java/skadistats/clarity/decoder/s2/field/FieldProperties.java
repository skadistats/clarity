package skadistats.clarity.decoder.s2.field;

import java.util.function.Supplier;

public class FieldProperties {

    private FieldType type;
    private Supplier<String> nameSupplier;

    public FieldProperties(FieldType type, Supplier<String> nameSupplier) {
        this.type = type;
        this.nameSupplier = nameSupplier;
    }

    public FieldType getType() {
        return type;
    }

    public String getName() {
        return nameSupplier.get();
    }

}
