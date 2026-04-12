package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

@RegisterDecoder
public final class FloatCoordDecoder extends Decoder {

    public static Float decode(BitStream bs) {
        return bs.readBitCoord();
    }

}
