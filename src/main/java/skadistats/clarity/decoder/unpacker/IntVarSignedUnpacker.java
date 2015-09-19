package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class IntVarSignedUnpacker implements Unpacker<Integer> {

    @Override
    public Integer unpack(BitStream bs) {
        return bs.readVarSInt();
    }
    
}
