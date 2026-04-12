package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

@RegisterDecoder
public final class FloatNoScaleDecoder extends Decoder {

    public static Float decode(BitStream bs) {
        return Float.intBitsToFloat(bs.readUBitInt(32));
    }

}
