package skadistats.clarity.decoder.s2.field.impl;

import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.s2.Field;
import skadistats.clarity.decoder.s2.S2UnpackerFactory;
import skadistats.clarity.decoder.s2.field.FieldProperties;
import skadistats.clarity.decoder.s2.field.UnpackerProperties;
import skadistats.clarity.decoder.unpacker.Unpacker;

public class ListField extends Field {

    private final Unpacker<?> lengthUnpacker;
    private final Field elementField;

    public ListField(FieldProperties fieldProperties, UnpackerProperties unpackerProperties, Unpacker<?> lengthUnpacker, Field elementField) {
        super(fieldProperties, unpackerProperties);
        this.lengthUnpacker = lengthUnpacker;
        this.elementField = elementField;
    }

    @Override
    public Unpacker<?> getUnpacker() {
        return lengthUnpacker;
    }

    @Override
    public Field getChild(int idx) {
        return elementField;
    }

    @Override
    public Integer getChildIndex(String name) {
        return Util.stringToArrayIdx(name);
    }

}
