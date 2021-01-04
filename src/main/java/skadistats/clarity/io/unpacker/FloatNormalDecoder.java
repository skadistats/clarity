package skadistats.clarity.io.unpacker;

import skadistats.clarity.io.bitstream.BitStream;

public class FloatNormalDecoder implements Decoder<Float> {

    @Override
    public Float decode(BitStream bs) {
        return bs.readBitNormal();
    }

}
