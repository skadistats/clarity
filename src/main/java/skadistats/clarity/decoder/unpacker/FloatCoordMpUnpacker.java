package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class FloatCoordMpUnpacker implements Unpacker<Float> {

    private final boolean integral;
    private final boolean lowPrecision;

    public FloatCoordMpUnpacker(boolean integral, boolean lowPrecision) {
        this.integral = integral;
        this.lowPrecision = lowPrecision;
    }

    @Override
    public Float unpack(BitStream bs) {
        return bs.readCoordMp(bs, integral, lowPrecision);
    }

}
