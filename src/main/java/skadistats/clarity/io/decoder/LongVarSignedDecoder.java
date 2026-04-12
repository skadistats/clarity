package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

@RegisterDecoder
public final class LongVarSignedDecoder extends Decoder {

    public static Long decode(BitStream bs) {
        return bs.readVarSLong();
    }

}
