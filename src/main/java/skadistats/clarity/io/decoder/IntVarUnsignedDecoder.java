package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.state.PrimitiveType;

@RegisterDecoder
public final class IntVarUnsignedDecoder extends Decoder {

    public static Integer decode(BitStream bs) {
        return bs.readVarUInt();
    }

    public static void decodeInto(BitStream bs, byte[] data, int offset) {
        PrimitiveType.INT_VH.set(data, offset, bs.readVarUInt());
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.INT;
    }

}
