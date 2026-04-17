package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.state.PrimitiveType;

@RegisterDecoder
public final class FloatCoordDecoder extends Decoder {

    public static Float decode(BitStream bs) {
        return bs.readBitCoord();
    }

    public static void decodeInto(BitStream bs, byte[] data, int offset) {
        PrimitiveType.FLOAT_VH.set(data, offset, bs.readBitCoord());
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.FLOAT;
    }

}
