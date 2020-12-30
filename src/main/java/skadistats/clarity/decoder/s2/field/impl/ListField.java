package skadistats.clarity.decoder.s2.field.impl;

import skadistats.clarity.decoder.s2.field.AccessorFunction;
import skadistats.clarity.decoder.s2.Field;
import skadistats.clarity.decoder.s2.field.FieldProperties;
import skadistats.clarity.decoder.s2.field.FieldType;
import skadistats.clarity.decoder.s2.field.UnpackerProperties;
import skadistats.clarity.decoder.unpacker.Unpacker;

public class ListField extends Field {

    private final Unpacker<?> unpacker;
    private final Field elementField;

    public ListField(FieldProperties fieldProperties, UnpackerProperties unpackerProperties, Unpacker<?> unpacker, Field elementField) {
        super(fieldProperties, unpackerProperties);
        this.unpacker = unpacker;
        this.elementField = elementField;
    }

    private AccessorFunction<Field> fieldAccessor = new AccessorFunction<Field>() {
        @Override
        public AccessorFunction<Field> down(int i) {
            return elementField.getFieldAccessor();
        }
        @Override
        public Field get() {
            return ListField.this;
        }
    };

    @Override
    public AccessorFunction<Field> getFieldAccessor() {
        return fieldAccessor;
    }

    private AccessorFunction<Unpacker<?>> unpackerAccessor = new AccessorFunction<Unpacker<?>>() {
        @Override
        public AccessorFunction<Unpacker<?>> down(int i) {
            return elementField.getUnpackerAccessor();
        }
        @Override
        public Unpacker<?> get() {
            return unpacker;
        }
    };

    @Override
    public AccessorFunction<Unpacker<?>> getUnpackerAccessor() {
        return unpackerAccessor;
    }

    private AccessorFunction<FieldType> typeAccessor = new AccessorFunction<FieldType>() {
        @Override
        public AccessorFunction<FieldType> down(int i) {
            return elementField.getTypeAccessor();
        }
        @Override
        public FieldType get() {
            return fieldProperties.getType();
        }
    };

    @Override
    public AccessorFunction<FieldType> getTypeAccessor() {
        return typeAccessor;
    }

}
