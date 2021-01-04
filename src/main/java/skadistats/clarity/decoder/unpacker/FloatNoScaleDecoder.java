package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class FloatNoScaleDecoder implements Decoder<Float> {

    @Override
    public Float decode(BitStream bs) {
        return Float.intBitsToFloat(bs.readUBitInt(32));
    }

}
