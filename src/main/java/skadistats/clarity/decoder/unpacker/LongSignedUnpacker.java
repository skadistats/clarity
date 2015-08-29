package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.BitStream;

public class LongSignedUnpacker implements Unpacker<Long> {

    private final int nBits;

    public LongSignedUnpacker(int nBits) {
        this.nBits = nBits;
    }

    @Override
    public Long unpack(BitStream bs) {
        return bs.readSBitLong(nBits);
    }
    
}
