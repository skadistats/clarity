package skadistats.clarity.model.s2;

public class Field {

    private final FieldType type;
    private final int encodeFlags;
    private final Integer bitCount;
    private final Float lowValue;
    private final Float highValue;
    private final Serializer serializer;

    public Field(FieldType type, int encodeFlags, Integer bitCount, Float lowValue, Float highValue, Serializer serializer) {
        this.type = type;
        this.encodeFlags = encodeFlags;
        this.bitCount = bitCount;
        this.lowValue = lowValue;
        this.highValue = highValue;
        this.serializer = serializer;
    }

}
