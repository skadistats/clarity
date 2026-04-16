package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.state.PrimitiveType;

@RegisterDecoder
public final class LongVarSignedDecoder extends Decoder {

    public static Long decode(BitStream bs) {
        return bs.readVarSLong();
    }

    public static void decodeInto(BitStream bs, byte[] data, int offset) {
        PrimitiveType.LONG_VH.set(data, offset, bs.readVarSLong());
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.LONG;
    }

}
