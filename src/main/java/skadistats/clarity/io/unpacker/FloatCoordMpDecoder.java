package skadistats.clarity.io.unpacker;

import skadistats.clarity.io.bitstream.BitStream;

public class FloatCoordMpDecoder implements Decoder<Float> {

    private final boolean integral;
    private final boolean lowPrecision;

    public FloatCoordMpDecoder(boolean integral, boolean lowPrecision) {
        this.integral = integral;
        this.lowPrecision = lowPrecision;
    }

    @Override
    public Float decode(BitStream bs) {
        return bs.readCoordMp(bs, integral, lowPrecision);
    }

}
