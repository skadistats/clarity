package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Vector;
import skadistats.clarity.model.state.PrimitiveType;

@RegisterDecoder
public final class QAngleNoBitCountDecoder extends Decoder {

    public static Vector decode(BitStream bs) {
        var v = new float[3];
        var b0 = bs.readBitFlag();
        var b1 = bs.readBitFlag();
        var b2 = bs.readBitFlag();
        if (b0) v[0] = bs.readBitCoord();
        if (b1) v[1] = bs.readBitCoord();
        if (b2) v[2] = bs.readBitCoord();
        return new Vector(v);
    }

    public static void decodeInto(BitStream bs, byte[] data, int offset) {
        // Matches decode(): absent components default to 0f, written explicitly to
        // keep byte[] layout identical to `PrimitiveType.VectorType.write(new Vector(v))`.
        var b0 = bs.readBitFlag();
        var b1 = bs.readBitFlag();
        var b2 = bs.readBitFlag();
        PrimitiveType.FLOAT_VH.set(data, offset,     b0 ? bs.readBitCoord() : 0f);
        PrimitiveType.FLOAT_VH.set(data, offset + 4, b1 ? bs.readBitCoord() : 0f);
        PrimitiveType.FLOAT_VH.set(data, offset + 8, b2 ? bs.readBitCoord() : 0f);
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 3);
    }

}
