package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class FloatCoordDecoder implements Decoder<Float> {

    @Override
    public Float decode(BitStream bs) {
        return bs.readBitCoord();
    }

}
