package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Vector;
import skadistats.clarity.state.PrimitiveType;

@RegisterDecoder
public final class VectorNormalDecoder extends Decoder {

    public static Vector decode(BitStream bs) {
        return new Vector(
            bs.read3BitNormal()
        );
    }

    public static void decodeInto(BitStream bs, byte[] data, int offset) {
        // Inlined read3BitNormal — writes directly to byte[] without allocating float[3].
        var hasX = bs.readBitFlag();
        var hasY = bs.readBitFlag();
        var x = hasX ? bs.readBitNormal() : 0f;
        var y = hasY ? bs.readBitNormal() : 0f;
        var sign = bs.readBitFlag();
        var p = x * x + y * y;
        float z = 0f;
        if (p < 1.0f) z = (float) Math.sqrt(1.0f - p);
        if (sign) z = -z;
        PrimitiveType.FLOAT_VH.set(data, offset, x);
        PrimitiveType.FLOAT_VH.set(data, offset + 4, y);
        PrimitiveType.FLOAT_VH.set(data, offset + 8, z);
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 3);
    }

}
