package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.BitStream;

public class LongUnsignedUnpacker implements Unpacker<Long> {

    private final int nBits;

    public LongUnsignedUnpacker(int nBits) {
        this.nBits = nBits;
    }

    @Override
    public Long unpack(BitStream bs) {
        return bs.readUBitLong(nBits);
    }
    
}
