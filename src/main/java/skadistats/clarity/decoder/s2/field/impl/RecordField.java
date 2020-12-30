package skadistats.clarity.decoder.s2.field.impl;

import skadistats.clarity.decoder.s2.Field;
import skadistats.clarity.decoder.s2.Serializer;
import skadistats.clarity.decoder.s2.field.AccessorFunction;
import skadistats.clarity.decoder.s2.field.FieldProperties;
import skadistats.clarity.decoder.s2.field.FieldType;
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

    public Serializer getSerializer() {
        return serializer;
    }

    private AccessorFunction<Field> fieldAccessor = new AccessorFunction<Field>() {
        @Override
        public AccessorFunction<Field> down(int i) {
            return serializer.getField(i).getFieldAccessor();
        }
        @Override
        public Field get() {
            return RecordField.this;
        }
    };

    @Override
    public AccessorFunction<Field> getFieldAccessor() {
        return fieldAccessor;
    }

    private AccessorFunction<Unpacker<?>> unpackerAccessor = new AccessorFunction<Unpacker<?>>() {
        @Override
        public AccessorFunction<Unpacker<?>> down(int i) {
            return serializer.getField(i).getUnpackerAccessor();
        }
        @Override
        public Unpacker<?> get() {
            return baseUnpacker;
        }
    };

    @Override
    public AccessorFunction<Unpacker<?>> getUnpackerAccessor() {
        return unpackerAccessor;
    }

    private AccessorFunction<FieldType> typeAccessor = new AccessorFunction<FieldType>() {
        @Override
        public AccessorFunction<FieldType> down(int i) {
            return serializer.getField(i).getTypeAccessor();
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
        return serializer.getFieldIndex(name);
    }


}
