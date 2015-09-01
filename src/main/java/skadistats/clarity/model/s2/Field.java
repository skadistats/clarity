package skadistats.clarity.model.s2;

import skadistats.clarity.decoder.s2.S2UnpackerFactory;
import skadistats.clarity.decoder.unpacker.Unpacker;

public class Field {

    private final FieldType type;
    private final String name;
    private final String sendNode;
    private final Integer encodeFlags;
    private final Integer bitCount;
    private final Float lowValue;
    private final Float highValue;
    private final Serializer serializer;
    private final String encoder;
    private final Unpacker baseUnpacker;
    private final Unpacker elementUnpacker;

    public Field(FieldType type, String name, String sendNode, Integer encodeFlags, Integer bitCount, Float lowValue, Float highValue, Serializer serializer, String encoder) {
        this.type = type;
        this.name = name;
        this.sendNode = sendNode;
        this.encodeFlags = encodeFlags;
        this.bitCount = bitCount;
        this.lowValue = lowValue;
        this.highValue = highValue;
        this.serializer = serializer;
        this.encoder = encoder;
        this.baseUnpacker = S2UnpackerFactory.createUnpacker(this, type.getBaseType());
        if (type.isGenericArray()) {
            this.elementUnpacker = S2UnpackerFactory.createUnpacker(this, type.getGenericType().getBaseType());
        } else {
            this.elementUnpacker = null;
        }
    }

    public FieldType getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getSendNode() {
        return sendNode;
    }

    public Integer getEncodeFlags() {
        return encodeFlags;
    }

    public int getEncodeFlagsOrDefault(int defaultValue) {
        return encodeFlags != null ? encodeFlags.intValue() : defaultValue;
    }

    public Integer getBitCount() {
        return bitCount;
    }

    public int getBitCountOrDefault(int defaultValue) {
        return bitCount != null ? bitCount.intValue() : defaultValue;
    }

    public Float getLowValue() {
        return lowValue;
    }

    public float getLowValueOrDefault(float defaultValue) {
        return lowValue != null ? lowValue.floatValue() : defaultValue;
    }

    public Float getHighValue() {
        return highValue;
    }

    public float getHighValueOrDefault(float defaultValue) {
        return highValue != null ? highValue.floatValue() : defaultValue;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    public String getEncoder() {
        return encoder;
    }

    public Unpacker getBaseUnpacker() {
        return baseUnpacker;
    }

    public Unpacker getElementUnpacker() {
        return elementUnpacker;
    }

}
