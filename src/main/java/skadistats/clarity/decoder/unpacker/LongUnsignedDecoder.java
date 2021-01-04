package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class LongUnsignedDecoder implements Decoder<Long> {

    private final int nBits;

    public LongUnsignedDecoder(int nBits) {
        this.nBits = nBits;
    }

    @Override
    public Long decode(BitStream bs) {
        return bs.readUBitLong(nBits);
    }

}
