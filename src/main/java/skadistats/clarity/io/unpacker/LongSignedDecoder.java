package skadistats.clarity.io.unpacker;

import skadistats.clarity.io.bitstream.BitStream;

public class LongSignedDecoder implements Decoder<Long> {

    private final int nBits;

    public LongSignedDecoder(int nBits) {
        this.nBits = nBits;
    }

    @Override
    public Long decode(BitStream bs) {
        return bs.readSBitLong(nBits);
    }

}
