package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.BitStream;

public class LongVarSignedUnpacker implements Unpacker<Long> {

    @Override
    public Long unpack(BitStream bs) {
        return bs.readVarSLong();
    }
    
}
