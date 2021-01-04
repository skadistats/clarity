package skadistats.clarity.io.unpacker;

import skadistats.clarity.io.bitstream.BitStream;

public class BoolDecoder implements Decoder<Boolean> {

    @Override
    public Boolean decode(BitStream bs) {
        return bs.readBitFlag();
    }

}
