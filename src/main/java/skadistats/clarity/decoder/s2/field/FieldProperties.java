package skadistats.clarity.decoder.s2.field;

import skadistats.clarity.decoder.s2.Serializer;

import java.util.function.Supplier;

public class FieldProperties {

    private final FieldType type;
    private final Supplier<String> nameSupplier;
    private final Serializer serializer;

    public FieldProperties(FieldType type, Supplier<String> nameSupplier, Serializer serializer) {
        this.type = type;
        this.nameSupplier = nameSupplier;
        this.serializer = serializer;
    }

    public FieldType getType() {
        return type;
    }

    public String getName() {
        return nameSupplier.get();
    }

    public Serializer getSerializer() {
        return serializer;
    }

}
