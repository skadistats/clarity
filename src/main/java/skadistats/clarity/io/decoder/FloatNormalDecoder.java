package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.state.PrimitiveType;

@RegisterDecoder
public final class FloatNormalDecoder extends Decoder {

    public static Float decode(BitStream bs) {
        return bs.readBitNormal();
    }

    public static void decodeInto(BitStream bs, byte[] data, int offset) {
        PrimitiveType.FLOAT_VH.set(data, offset, bs.readBitNormal());
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.FLOAT;
    }

}
