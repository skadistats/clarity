package skadistats.clarity.model.s2.field;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.model.s2.Field;
import skadistats.clarity.model.s2.FieldType;
import skadistats.clarity.model.s2.SerializerProperties;

public class ValueField extends Field {

    private final Decoder decoder;
    private final SerializerProperties serializerProperties;

    public ValueField(FieldType fieldType, Decoder decoder, SerializerProperties serializerProperties) {
        super(fieldType);
        this.decoder = decoder;
        this.serializerProperties = serializerProperties;
    }

    @Override
    public SerializerProperties getSerializerProperties() {
        return serializerProperties;
    }

    @Override
    public Decoder getDecoder() {
        return decoder;
    }

    @Override
    public boolean isPrimitiveLeaf() {
        return true;
    }

}
