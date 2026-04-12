package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

@RegisterDecoder
public final class StringZeroTerminatedDecoder extends Decoder {

    public static String decode(BitStream bs) {
        return bs.readString(Integer.MAX_VALUE);
    }

}
