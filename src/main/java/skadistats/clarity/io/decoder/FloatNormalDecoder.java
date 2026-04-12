package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

@RegisterDecoder
public final class FloatNormalDecoder extends Decoder {

    public static Float decode(BitStream bs) {
        return bs.readBitNormal();
    }

}
