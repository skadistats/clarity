package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.state.PrimitiveType;

@RegisterDecoder
public final class IntSignedDecoder extends Decoder {

    private final int nBits;

    public IntSignedDecoder(int nBits) {
        this.nBits = nBits;
    }

    public static Integer decode(BitStream bs, IntSignedDecoder d) {
        return bs.readSBitInt(d.nBits);
    }

    public static void decodeInto(BitStream bs, IntSignedDecoder d, byte[] data, int offset) {
        PrimitiveType.INT_VH.set(data, offset, bs.readSBitInt(d.nBits));
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.INT;
    }

}
