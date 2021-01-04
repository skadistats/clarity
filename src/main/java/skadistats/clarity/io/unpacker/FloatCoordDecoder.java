package skadistats.clarity.io.unpacker;

import skadistats.clarity.io.bitstream.BitStream;

public class FloatCoordDecoder implements Decoder<Float> {

    @Override
    public Float decode(BitStream bs) {
        return bs.readBitCoord();
    }

}
