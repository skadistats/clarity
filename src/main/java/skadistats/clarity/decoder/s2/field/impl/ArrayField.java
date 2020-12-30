package skadistats.clarity.decoder.s2.field.impl;

import skadistats.clarity.decoder.s2.field.AccessorFunction;
import skadistats.clarity.decoder.s2.Field;
import skadistats.clarity.decoder.s2.field.FieldProperties;
import skadistats.clarity.decoder.s2.field.FieldType;
import skadistats.clarity.decoder.unpacker.Unpacker;

public class ArrayField extends Field {

    private final Field elementField;

    public ArrayField(FieldProperties fieldProperties, Field elementField, int length) {
        super(fieldProperties, null);
        this.elementField = elementField;
    }

    private AccessorFunction<Field> fieldAccessor = new AccessorFunction<Field>() {
        @Override
        public AccessorFunction<Field> down(int i) {
            return elementField.getFieldAccessor();
        }
        @Override
        public Field get() {
            return ArrayField.this;
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
            throw new UnsupportedOperationException();
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
