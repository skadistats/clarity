package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.BitStream;

public class IntUnsignedUnpacker implements Unpacker<Integer> {

    private final int nBits;

    public IntUnsignedUnpacker(int nBits) {
        this.nBits = nBits;
    }

    @Override
    public Integer unpack(BitStream bs) {
        return bs.readUBitInt(nBits);
    }
    
}
