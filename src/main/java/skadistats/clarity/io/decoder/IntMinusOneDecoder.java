package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

@RegisterDecoder
public final class IntMinusOneDecoder extends Decoder {

    public static Integer decode(BitStream bs) {
        return bs.readVarUInt() - 1;
    }

}
