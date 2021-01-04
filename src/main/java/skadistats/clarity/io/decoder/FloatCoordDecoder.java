package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

public class FloatCoordDecoder implements Decoder<Float> {

    @Override
    public Float decode(BitStream bs) {
        return bs.readBitCoord();
    }

}
