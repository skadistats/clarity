package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

@RegisterDecoder
public final class IntSignedDecoder extends Decoder {

    private final int nBits;

    public IntSignedDecoder(int nBits) {
        this.nBits = nBits;
    }

    public static Integer decode(BitStream bs, IntSignedDecoder d) {
        return bs.readSBitInt(d.nBits);
    }

}
