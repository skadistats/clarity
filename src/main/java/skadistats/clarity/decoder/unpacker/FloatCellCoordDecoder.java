package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class FloatCellCoordDecoder implements Decoder<Float> {

    private final int nBits;
    private final boolean integral;
    private final boolean lowPrecision;

    public FloatCellCoordDecoder(int nBits, boolean integral, boolean lowPrecision) {
        this.nBits = nBits;
        this.integral = integral;
        this.lowPrecision = lowPrecision;
    }

    @Override
    public Float decode(BitStream bs) {
        return bs.readCellCoord(nBits, integral, lowPrecision);
    }

}
