package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class IntMinusOneUnpacker implements Unpacker<Integer> {

    @Override
    public Integer unpack(BitStream bs) {
        return bs.readVarUInt() - 1;
    }
    
}
