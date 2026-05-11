package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.state.PrimitiveType;

@RegisterDecoder
public final class IntVarUnsignedDecoder extends Decoder {

    public static Integer decode(BitStream bs) {
        return bs.readVarUInt();
    }

    public static void decodeInto(BitStream bs, byte[] data, int offset) {
        PrimitiveType.INT_VH.set(data, offset, bs.readVarUInt());
    }

    public static void skip(BitStream bs) {
        bs.skipVarUInt();
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.INT;
    }

}
