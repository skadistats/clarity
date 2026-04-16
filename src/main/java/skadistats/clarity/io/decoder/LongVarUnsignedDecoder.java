package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.state.PrimitiveType;

@RegisterDecoder
public final class LongVarUnsignedDecoder extends Decoder {

    public static Long decode(BitStream bs) {
        return bs.readVarULong();
    }

    public static void decodeInto(BitStream bs, byte[] data, int offset) {
        PrimitiveType.LONG_VH.set(data, offset, bs.readVarULong());
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.LONG;
    }

}
