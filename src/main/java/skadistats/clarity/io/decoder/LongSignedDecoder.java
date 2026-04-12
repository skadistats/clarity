package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

@RegisterDecoder
public final class LongSignedDecoder extends Decoder {

    private final int nBits;

    public LongSignedDecoder(int nBits) {
        this.nBits = nBits;
    }

    public static Long decode(BitStream bs, LongSignedDecoder d) {
        return bs.readSBitLong(d.nBits);
    }

}
