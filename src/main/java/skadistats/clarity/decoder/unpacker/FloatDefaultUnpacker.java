package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class FloatDefaultUnpacker implements Unpacker<Float> {

    private final int bitCount;
    private final float minValue;
    private final float maxValue;
    private final float decodeMultiplier;

    public FloatDefaultUnpacker(int bitCount, float minValue, float maxValue) {
        this.bitCount = bitCount;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.decodeMultiplier = 1.0f / ((1 << bitCount) - 1);
    }

    @Override
    public Float unpack(BitStream bs) {
        float v = bs.readUBitInt(bitCount) * decodeMultiplier;
        return minValue + (maxValue - minValue) * v;
    }

}
