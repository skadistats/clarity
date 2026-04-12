package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

@RegisterDecoder
public final class BoolDecoder extends Decoder {

    public static Boolean decode(BitStream bs) {
        return bs.readBitFlag();
    }

}
