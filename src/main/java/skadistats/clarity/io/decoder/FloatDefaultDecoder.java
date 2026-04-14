package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.state.PrimitiveType;

@RegisterDecoder
public final class FloatDefaultDecoder extends Decoder {

    private final int bitCount;
    private final float minValue;
    private final float maxValue;
    private final float decodeMultiplier;

    public FloatDefaultDecoder(int bitCount, float minValue, float maxValue) {
        this.bitCount = bitCount;
        this.minValue = minValue;
        this.maxValue = maxValue;
        this.decodeMultiplier = 1.0f / ((1 << bitCount) - 1);
    }

    public static Float decode(BitStream bs, FloatDefaultDecoder d) {
        var v = bs.readUBitInt(d.bitCount) * d.decodeMultiplier;
        return d.minValue + (d.maxValue - d.minValue) * v;
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.FLOAT;
    }

}
