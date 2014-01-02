package clarity.decoder.prop;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import clarity.decoder.EntityBitStream;
import clarity.model.Prop;
import clarity.model.PropFlag;

public class FloatDecoder implements PropDecoder<Float> {

    enum FloatType {
        MP_NONE,
        MP_LOW_PRECISION,
        MP_INTEGRAL
    };
    
    @Override
    public Float decode(EntityBitStream stream, Prop prop) {
        if (prop.isFlagSet(PropFlag.COORD)) {
            return decodeCoord(stream);
        } else if (prop.isFlagSet(PropFlag.COORD_MP)) {
            return decodeFloatCoordMp(stream, FloatType.MP_NONE);
        } else if (prop.isFlagSet(PropFlag.COORD_MP_LOW_PRECISION)) {
            return decodeFloatCoordMp(stream, FloatType.MP_LOW_PRECISION);
        } else if (prop.isFlagSet(PropFlag.COORD_MP_INTEGRAL)) {
            return decodeFloatCoordMp(stream, FloatType.MP_INTEGRAL);
        } else if (prop.isFlagSet(PropFlag.NO_SCALE)) {
            return decodeNoScale(stream);
        } else if (prop.isFlagSet(PropFlag.NORMAL)) {
            return decodeNormal(stream);
        } else if (prop.isFlagSet(PropFlag.CELL_COORD)) {
            return decodeCellCoord(stream, prop.getNumBits());
        } else if (prop.isFlagSet(PropFlag.CELL_COORD_INTEGRAL)) {
            return decodeCellCoordIntegral(stream, prop.getNumBits());
        } else {
            return decodeDefault(stream, prop.getNumBits(), prop.getHighValue(), prop.getLowValue());
        }
    }

    public Float decodeCoord(EntityBitStream stream) {
        boolean _i = stream.readBit(); // integer component present?
        boolean _f = stream.readBit(); // fractional component present?

        if (!(_i || _f)) {
            return 0.0f;
        }
        boolean isNegative = stream.readBit();
        int i = 0;
        int f = 0;
        if (_i) {
            i = stream.readNumericBits(14) + 1;
        }
        if (_f) {
            f = stream.readNumericBits(5);
        }
        float v = i + 0.03125f * f;
        return isNegative ? -v : v;
    }
    
    public Float decodeFloatCoordMp(EntityBitStream stream, FloatType type) {
        int value;
        if (type == FloatType.MP_LOW_PRECISION || type == FloatType.MP_NONE) {
            value = stream.readNumericBits(1) + 2 * stream.readNumericBits(1) + 4 * stream.readNumericBits(1);
            throw new RuntimeException("edith says 'PLEASE NO!'");
        } else if (type == FloatType.MP_INTEGRAL) {
            int a = stream.readNumericBits(1);
            int b = stream.readNumericBits(1);
            a = a + 2 * b;

            if (b == 0) {
                return 0.0f;
            } else {
                if (a != 0) {
                    value = stream.readNumericBits(12);
                } else {
                    value = stream.readNumericBits(15);
                }
                if ((value & 1) == 1) {
                    value = -((value >>> 1) + 1);
                } else {
                    value = (value >>> 1) + 1;
                }
                return (float) value;
            }
        } else {
            throw new RuntimeException("unable to decode float of type " + type);
        }
    }

    public Float decodeNoScale(EntityBitStream stream) {
        return ByteBuffer.wrap(stream.readBits(32)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
    }

    public Float decodeNormal(EntityBitStream stream) {
        boolean isNegative = stream.readBit();
        int l = stream.readNumericBits(11);
        byte[] b = new byte[] { 0, 0, (byte) ((l & 0x0000ff00) >> 8), (byte) (l & 0x000000ff) };
        float v = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getFloat();
        v *= 4.885197850512946e-4f;
        return isNegative ? -v : v;
    }

    public Float decodeCellCoord(EntityBitStream stream, int numBits) {
        int v = stream.readNumericBits(numBits);
        return v + 0.01325f * stream.readNumericBits(5);
    }

    public Float decodeCellCoordIntegral(EntityBitStream stream, int numBits) {
        int v = stream.readNumericBits(numBits);
        return (float) v;
    }

    public Float decodeDefault(EntityBitStream stream, int numBits, float high, float low) {
        int t = stream.readNumericBits(numBits);
        float f = (float) t / (1 << numBits - 1);
        return f * (high - low) + low;
    }

}
