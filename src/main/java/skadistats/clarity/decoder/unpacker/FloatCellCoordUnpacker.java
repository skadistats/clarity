package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;

public class FloatCellCoordUnpacker implements Unpacker<Float> {

    private final int nBits;
    private final boolean integral;
    private final boolean lowPrecision;

    public FloatCellCoordUnpacker(int nBits, boolean integral, boolean lowPrecision) {
        this.nBits = nBits;
        this.integral = integral;
        this.lowPrecision = lowPrecision;
    }

    @Override
    public Float unpack(BitStream bs) {
        return bs.readCellCoord(nBits, integral, lowPrecision);
    }

}
