package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

public class BoolDecoder implements Decoder<Boolean> {

    @Override
    public Boolean decode(BitStream bs) {
        return bs.readBitFlag();
    }

}
