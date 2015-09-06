package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.BitStream;

public class LongFixedUnsignedUnpacker implements Unpacker<Long> {

    @Override
    public Long unpack(BitStream bs) {
        return bs.readUBitLong(64);
    }
    
}
