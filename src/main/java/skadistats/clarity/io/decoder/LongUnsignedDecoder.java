package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.state.PrimitiveType;

@RegisterDecoder
public final class LongUnsignedDecoder extends Decoder {

    private final int nBits;

    public LongUnsignedDecoder(int nBits) {
        this.nBits = nBits;
    }

    public static Long decode(BitStream bs, LongUnsignedDecoder d) {
        return bs.readUBitLong(d.nBits);
    }

    public static void decodeInto(BitStream bs, LongUnsignedDecoder d, byte[] data, int offset) {
        PrimitiveType.LONG_VH.set(data, offset, bs.readUBitLong(d.nBits));
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.LONG;
    }

}
