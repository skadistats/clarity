package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

@RegisterDecoder
public final class LongVarUnsignedDecoder extends Decoder {

    public static Long decode(BitStream bs) {
        return bs.readVarULong();
    }

}
