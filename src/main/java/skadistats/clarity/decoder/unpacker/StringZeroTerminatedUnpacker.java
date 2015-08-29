package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.BitStream;

public class StringZeroTerminatedUnpacker implements Unpacker<String> {

    @Override
    public String unpack(BitStream bs) {
        return bs.readString(Integer.MAX_VALUE);
    }
    
}
