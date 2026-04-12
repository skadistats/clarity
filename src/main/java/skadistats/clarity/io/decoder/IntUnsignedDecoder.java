package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

@RegisterDecoder
public final class IntUnsignedDecoder extends Decoder {

    private final int nBits;

    public IntUnsignedDecoder(int nBits) {
        this.nBits = nBits;
    }

    public static Integer decode(BitStream bs, IntUnsignedDecoder d) {
        return bs.readUBitInt(d.nBits);
    }

}
