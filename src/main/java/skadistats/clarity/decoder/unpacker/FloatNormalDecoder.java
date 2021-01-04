package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class FloatNormalDecoder implements Decoder<Float> {

    @Override
    public Float decode(BitStream bs) {
        return bs.readBitNormal();
    }

}
