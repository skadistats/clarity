package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.state.PrimitiveType;

@RegisterDecoder
public final class LongSignedDecoder extends Decoder {

    private final int nBits;

    public LongSignedDecoder(int nBits) {
        this.nBits = nBits;
    }

    public static Long decode(BitStream bs, LongSignedDecoder d) {
        return bs.readSBitLong(d.nBits);
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.LONG;
    }

}
