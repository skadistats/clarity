package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.state.PrimitiveType;

@RegisterDecoder
public final class IntMinusOneDecoder extends Decoder {

    public static Integer decode(BitStream bs) {
        return bs.readVarUInt() - 1;
    }

    public static void decodeInto(BitStream bs, byte[] data, int offset) {
        PrimitiveType.INT_VH.set(data, offset, bs.readVarUInt() - 1);
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.INT;
    }

}
