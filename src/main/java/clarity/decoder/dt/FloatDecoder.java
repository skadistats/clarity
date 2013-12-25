package clarity.decoder.dt;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import clarity.decoder.EntityBitStream;
import clarity.model.Prop;
import clarity.model.PropFlag;

public class FloatDecoder implements DtDecoder<Float> {

	@Override
	public Float decode(EntityBitStream stream, Prop prop) {
		if (prop.isFlagSet(PropFlag.COORD)) {
			return decodeCoord(stream);
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
	
	public Float decodeNoScale(EntityBitStream stream) {
		return ByteBuffer.wrap(stream.readBits(32)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
	}

	public Float decodeNormal(EntityBitStream stream) {
		boolean isNegative = stream.readBit();
        int l = stream.readNumericBits(11);
        byte[] b = new byte[] {0, 0, (byte)((l & 0x0000ff00) >> 8), (byte)(l & 0x000000ff) };
		float v = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).getFloat();
		if (l < 0) {
            v += 4.2949673e9f;
		}
        v *= 4.885197850512946e-4f;
        return isNegative ? -v : v;
	}

	public Float decodeCellCoord(EntityBitStream stream, int numBits) {
        int v = stream.readNumericBits(numBits);
        return v + 0.01325f * stream.readNumericBits(5);
	}

	public Float decodeCellCoordIntegral(EntityBitStream stream, int numBits) {
        int v = stream.readNumericBits(numBits);
        return (float)v;
	}

	public Float decodeDefault(EntityBitStream stream, int numBits, float high, float low) {
        int t = stream.readNumericBits(numBits);
        float f = (float)t / (1 << numBits - 1);
        return f * (high - low) + low;
	}
	
}
