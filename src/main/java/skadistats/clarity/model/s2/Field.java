package skadistats.clarity.model.s2;

public class Field {

    private final FieldType type;
    private final String name;
    private final String sendNode;
    private final int encodeFlags;
    private final int bitCount;
    private final Float lowValue;
    private final Float highValue;
    private final Serializer serializer;
    private final String encoder;

    public Field(FieldType type, String name, String sendNode, int encodeFlags, int bitCount, Float lowValue, Float highValue, Serializer serializer, String encoder) {
        this.type = type;
        this.name = name;
        this.sendNode = sendNode;
        this.encodeFlags = encodeFlags;
        this.bitCount = bitCount;
        this.lowValue = lowValue;
        this.highValue = highValue;
        this.serializer = serializer;
        this.encoder = encoder;
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

    public int getEncodeFlags() {
        return encodeFlags;
    }

    public int getBitCount() {
        return bitCount;
    }

    public Float getLowValue() {
        return lowValue;
    }

    public Float getHighValue() {
        return highValue;
    }

    public Serializer getSerializer() {
        return serializer;
    }

    public String getEncoder() {
        return encoder;
    }

}
