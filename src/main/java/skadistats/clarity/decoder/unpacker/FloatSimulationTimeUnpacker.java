package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class FloatSimulationTimeUnpacker implements Unpacker<Float> {

    private static final float FRAME_TIME = (1.0f / 30.0f);

    @Override
    public Float unpack(BitStream bs) {
        return bs.readVarULong() * FRAME_TIME;
    }
    
}
