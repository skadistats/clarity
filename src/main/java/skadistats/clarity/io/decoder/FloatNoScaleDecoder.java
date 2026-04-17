package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.state.PrimitiveType;

@RegisterDecoder
public final class FloatNoScaleDecoder extends Decoder {

    public static Float decode(BitStream bs) {
        return Float.intBitsToFloat(bs.readUBitInt(32));
    }

    public static void decodeInto(BitStream bs, byte[] data, int offset) {
        // Write the raw 32-bit int representation — PrimitiveType.FLOAT uses FLOAT_VH
        // which expects a float, so we round-trip through intBitsToFloat to match.
        PrimitiveType.FLOAT_VH.set(data, offset, Float.intBitsToFloat(bs.readUBitInt(32)));
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.FLOAT;
    }

}
