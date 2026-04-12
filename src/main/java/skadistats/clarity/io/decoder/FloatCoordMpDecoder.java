package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;

@RegisterDecoder
public final class FloatCoordMpDecoder extends Decoder {

    private final boolean integral;
    private final boolean lowPrecision;

    public FloatCoordMpDecoder(boolean integral, boolean lowPrecision) {
        this.integral = integral;
        this.lowPrecision = lowPrecision;
    }

    public static Float decode(BitStream bs, FloatCoordMpDecoder d) {
        return bs.readCoordMp(bs, d.integral, d.lowPrecision);
    }

}
