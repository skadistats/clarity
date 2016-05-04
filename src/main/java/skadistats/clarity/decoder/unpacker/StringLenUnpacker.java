package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class StringLenUnpacker implements Unpacker<String> {

    @Override
    public String unpack(BitStream bs) {
        return bs.readString(bs.readUBitInt(9));
    }
    
}
