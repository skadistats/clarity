package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Vector;
import skadistats.clarity.model.state.PrimitiveType;

@RegisterDecoder
public final class QAngleNoScaleDecoder extends Decoder {

    public static Vector decode(BitStream bs) {
        var v = new float[3];
        v[0] = Float.intBitsToFloat(bs.readUBitInt(32));
        v[1] = Float.intBitsToFloat(bs.readUBitInt(32));
        v[2] = Float.intBitsToFloat(bs.readUBitInt(32));
        return new Vector(v);
    }

    public static void decodeInto(BitStream bs, byte[] data, int offset) {
        PrimitiveType.FLOAT_VH.set(data, offset,     Float.intBitsToFloat(bs.readUBitInt(32)));
        PrimitiveType.FLOAT_VH.set(data, offset + 4, Float.intBitsToFloat(bs.readUBitInt(32)));
        PrimitiveType.FLOAT_VH.set(data, offset + 8, Float.intBitsToFloat(bs.readUBitInt(32)));
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 3);
    }

}
