package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

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
