package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.BitStream;

public class IntVarUnsignedUnpacker implements Unpacker<Integer> {

    @Override
    public Integer unpack(BitStream bs) {
        return bs.readVarUInt();
    }
    
}
