package skadistats.clarity.decoder.s2.field.impl;

import skadistats.clarity.decoder.s2.Field;
import skadistats.clarity.decoder.s2.Serializer;
import skadistats.clarity.decoder.s2.field.FieldProperties;
import skadistats.clarity.decoder.s2.field.UnpackerProperties;
import skadistats.clarity.decoder.unpacker.Unpacker;

public class RecordField extends Field {

    private final Unpacker<?> baseUnpacker;
    private final Serializer serializer;

    public RecordField(FieldProperties fieldProperties, Serializer serializer) {
        this(fieldProperties, null, null, serializer);
    }

    public RecordField(FieldProperties fieldProperties, UnpackerProperties unpackerProperties, Unpacker<?> baseUnpacker, Serializer serializer) {
        super(fieldProperties, unpackerProperties);
        this.baseUnpacker = baseUnpacker;
        this.serializer = serializer;
    }

    @Override
    public Unpacker<?> getUnpacker() {
        return baseUnpacker;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    @Override
    public Field getChild(int idx) {
        return serializer.getField(idx);
    }

    @Override
    public Integer getChildIndex(String name) {
        return serializer.getFieldIndex(name);
    }


}
