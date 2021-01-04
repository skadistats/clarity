package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class BoolDecoder implements Decoder<Boolean> {

    @Override
    public Boolean decode(BitStream bs) {
        return bs.readBitFlag();
    }

}
