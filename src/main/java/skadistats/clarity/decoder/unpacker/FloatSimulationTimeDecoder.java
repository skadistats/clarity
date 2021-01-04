package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class FloatSimulationTimeDecoder implements Decoder<Float> {

    private static final float FRAME_TIME = (1.0f / 30.0f);

    @Override
    public Float decode(BitStream bs) {
        return bs.readVarULong() * FRAME_TIME;
    }

}
