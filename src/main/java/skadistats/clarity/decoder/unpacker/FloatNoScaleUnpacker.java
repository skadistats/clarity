package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class FloatNoScaleUnpacker implements Unpacker<Float> {

    @Override
    public Float unpack(BitStream bs) {
        return Float.intBitsToFloat(bs.readUBitInt(32));
    }

}
