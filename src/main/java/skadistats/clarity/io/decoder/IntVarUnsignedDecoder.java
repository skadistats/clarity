package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

@RegisterDecoder
public final class IntVarUnsignedDecoder extends Decoder {

    public static Integer decode(BitStream bs) {
        return bs.readVarUInt();
    }

}
