package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class FloatNormalUnpacker implements Unpacker<Float> {

    @Override
    public Float unpack(BitStream bs) {
        return bs.readBitNormal();
    }
    
}
