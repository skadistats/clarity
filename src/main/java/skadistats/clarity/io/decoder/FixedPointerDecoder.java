package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

@RegisterDecoder
public final class FixedPointerDecoder extends Decoder {

    public static Boolean decode(BitStream bs, FixedPointerDecoder d) {
        return bs.readBitFlag();
    }

    public static void skip(BitStream bs, FixedPointerDecoder d) {
        bs.skip(1);
    }

}
