package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

@RegisterDecoder
public final class IntVarSignedDecoder extends Decoder {

    public static Integer decode(BitStream bs) {
        return bs.readVarSInt();
    }

}
