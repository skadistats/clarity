package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.state.PrimitiveType;

@RegisterDecoder
public final class IntVarSignedDecoder extends Decoder {

    public static Integer decode(BitStream bs) {
        return bs.readVarSInt();
    }

    public static void decodeInto(BitStream bs, byte[] data, int offset) {
        PrimitiveType.INT_VH.set(data, offset, bs.readVarSInt());
    }

    public static void skip(BitStream bs) {
        bs.skipVarUInt();
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.INT;
    }

}
