package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

@RegisterDecoder
public final class StringLenDecoder extends Decoder {

    public static String decode(BitStream bs) {
        return bs.readString(bs.readUBitInt(9));
    }

}
