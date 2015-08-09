package skadistats.clarity.decoder.s1.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.s1.PropFlag;
import skadistats.clarity.model.s1.SendProp;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class FloatDecoder implements PropDecoder<Float> {

    private static final int COORD_INTEGER_BITS = 14;
    private static final int COORD_FRACTIONAL_BITS = 5;
    private static final int COORD_DENOMINATOR = (1 << COORD_FRACTIONAL_BITS);
    private static final float COORD_RESOLUTION = (1.0f / COORD_DENOMINATOR);

    private static final int COORD_INTEGER_BITS_MP = 11;
    private static final int COORD_FRACTIONAL_BITS_MP_LOWPRECISION = 3;
    private static final int COORD_DENOMINATOR_LOWPRECISION = (1 << COORD_FRACTIONAL_BITS_MP_LOWPRECISION);
    private static final float COORD_RESOLUTION_LOWPRECISION = (1.0f / COORD_DENOMINATOR_LOWPRECISION);

    private static final int NORMAL_FRACTIONAL_BITS = 11;
    private static final int NORMAL_DENOMINATOR = ((1 << NORMAL_FRACTIONAL_BITS) - 1);
    private static final float NORMAL_RESOLUTION = (1.0f / NORMAL_DENOMINATOR);
    
    @Override
    public Float decode(BitStream stream, SendProp prop) {
        int flags = prop.getFlags();
        if ((flags & PropFlag.COORD) != 0) {
            return decodeCoord(stream);
        } else if ((flags & PropFlag.COORD_MP) != 0) {
            return decodeFloatCoordMp(stream, false, false);
        } else if ((flags & PropFlag.COORD_MP_LOW_PRECISION) != 0) {
            return decodeFloatCoordMp(stream, false, true);
        } else if ((flags & PropFlag.COORD_MP_INTEGRAL) != 0) {
            return decodeFloatCoordMp(stream, true, false);
        } else if ((flags & PropFlag.NO_SCALE) != 0) {
            return decodeNoScale(stream);
        } else if ((flags & PropFlag.NORMAL) != 0) {
            return decodeNormal(stream);
        } else if ((flags & PropFlag.CELL_COORD) != 0) {
            return decodeCellCoord(stream, prop.getNumBits());
        } else if ((flags & PropFlag.CELL_COORD_INTEGRAL) != 0) {
            return decodeCellCoordIntegral(stream, prop.getNumBits());
        } else {
            return decodeDefault(stream, prop.getNumBits(), prop.getHighValue(), prop.getLowValue());
        }
    }

    public float decodeCoord(BitStream stream) {
        boolean hasInt = stream.readNumericBits(1) == 1; // integer component present?
        boolean hasFrac = stream.readNumericBits(1) == 1; // fractional component present?

        if (!(hasInt || hasFrac)) {
            return 0.0f;
        }
        boolean sign = stream.readNumericBits(1) == 1;
        int i = 0;
        int f = 0;
        if (hasInt) {
            i = stream.readNumericBits(COORD_INTEGER_BITS) + 1;
        }
        if (hasFrac) {
            f = stream.readNumericBits(COORD_FRACTIONAL_BITS);
        }
        float v = i + ((float) f * COORD_RESOLUTION);
        return sign ? -v : v;
    }

    public float decodeFloatCoordMp(BitStream stream, boolean integral, boolean lowPrecision) {
        int i = 0;
        int f = 0;
        boolean sign = false;
        float value = 0.0f;

        boolean inBounds = stream.readNumericBits(1) == 1;
        if (integral) {
            i = stream.readNumericBits(1);
            if (i != 0) {
                sign = stream.readNumericBits(1) == 1;
                value = stream.readNumericBits(inBounds ? COORD_INTEGER_BITS_MP : COORD_INTEGER_BITS) + 1;
            }
        } else {
            i = stream.readNumericBits(1);
            sign = stream.readNumericBits(1) == 1;
            if (i != 0) {
                i = stream.readNumericBits(inBounds ? COORD_INTEGER_BITS_MP : COORD_INTEGER_BITS) + 1;
            }
            f = stream.readNumericBits(lowPrecision ? COORD_FRACTIONAL_BITS_MP_LOWPRECISION : COORD_FRACTIONAL_BITS);
            value = i + ((float) f * (lowPrecision ? COORD_RESOLUTION_LOWPRECISION : COORD_RESOLUTION));
        }
        return sign ? -value : value;
    }

    public float decodeNoScale(BitStream stream) {
        return ByteBuffer.wrap(stream.readBits(32)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    public float decodeNormal(BitStream stream) {
        boolean isNegative = stream.readNumericBits(1) == 1;
        int l = stream.readNumericBits(NORMAL_FRACTIONAL_BITS);
        float v = (float) l * NORMAL_RESOLUTION;
        return isNegative ? -v : v;
    }

    public float decodeCellCoord(BitStream stream, int numBits) {
        int v = stream.readNumericBits(numBits);
        return v + COORD_RESOLUTION * stream.readNumericBits(COORD_FRACTIONAL_BITS);
    }

    public float decodeCellCoordIntegral(BitStream stream, int numBits) {
        int v = stream.readNumericBits(numBits);
        return (float) v;
    }

    public float decodeDefault(BitStream stream, int numBits, float high, float low) {
        int t = stream.readNumericBits(numBits);
        float f = (float) t / ((1 << numBits) - 1);
        return f * (high - low) + low;
    }

}
