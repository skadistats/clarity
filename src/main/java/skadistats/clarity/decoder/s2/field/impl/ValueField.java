package skadistats.clarity.decoder.s2.field.impl;

import skadistats.clarity.decoder.s2.Field;
import skadistats.clarity.decoder.s2.field.AccessorFunction;
import skadistats.clarity.decoder.s2.field.FieldProperties;
import skadistats.clarity.decoder.s2.field.FieldType;
import skadistats.clarity.decoder.s2.field.UnpackerProperties;
import skadistats.clarity.decoder.unpacker.Unpacker;

public class ValueField extends Field {

    private final Unpacker<?> unpacker;

    public ValueField(FieldProperties fieldProperties, UnpackerProperties unpackerProperties, Unpacker<?> unpacker) {
        super(fieldProperties, unpackerProperties);
        this.unpacker = unpacker;
    }

    private AccessorFunction<Field> fieldAccessor = new AccessorFunction<Field>() {
        @Override
        public AccessorFunction<Field> down(int i) {
            throw new UnsupportedOperationException();
        }
        @Override
        public Field get() {
            return ValueField.this;
        }
    };

    @Override
    public AccessorFunction<Field> getFieldAccessor() {
        return fieldAccessor;
    }

    private AccessorFunction<Unpacker<?>> unpackerAccessor = new AccessorFunction<Unpacker<?>>() {
        @Override
        public AccessorFunction<Unpacker<?>> down(int i) {
            throw new UnsupportedOperationException();
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
            throw new UnsupportedOperationException();
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

    @Override
    public Integer getFieldIndex(String name) {
        throw new UnsupportedOperationException();
    }

}
