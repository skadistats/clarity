package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class IntSignedUnpacker implements Unpacker<Integer> {

    private final int nBits;

    public IntSignedUnpacker(int nBits) {
        this.nBits = nBits;
    }

    @Override
    public Integer unpack(BitStream bs) {
        return bs.readSBitInt(nBits);
    }
    
}
