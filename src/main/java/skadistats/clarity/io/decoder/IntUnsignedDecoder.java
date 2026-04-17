package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.state.PrimitiveType;

@RegisterDecoder
public final class IntUnsignedDecoder extends Decoder {

    private final int nBits;

    public IntUnsignedDecoder(int nBits) {
        this.nBits = nBits;
    }

    public static Integer decode(BitStream bs, IntUnsignedDecoder d) {
        return bs.readUBitInt(d.nBits);
    }

    public static void decodeInto(BitStream bs, IntUnsignedDecoder d, byte[] data, int offset) {
        PrimitiveType.INT_VH.set(data, offset, bs.readUBitInt(d.nBits));
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.INT;
    }

}
