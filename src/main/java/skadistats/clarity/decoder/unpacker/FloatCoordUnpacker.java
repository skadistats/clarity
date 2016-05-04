package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class FloatCoordUnpacker implements Unpacker<Float> {

    @Override
    public Float unpack(BitStream bs) {
        return bs.readBitCoord();
    }
    
}
