package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.state.PrimitiveType;

@RegisterDecoder
public final class FloatCellCoordDecoder extends Decoder {

    private final int nBits;
    private final boolean integral;
    private final boolean lowPrecision;

    public FloatCellCoordDecoder(int nBits, boolean integral, boolean lowPrecision) {
        this.nBits = nBits;
        this.integral = integral;
        this.lowPrecision = lowPrecision;
    }

    public static Float decode(BitStream bs, FloatCellCoordDecoder d) {
        return bs.readCellCoord(d.nBits, d.integral, d.lowPrecision);
    }

    public static void decodeInto(BitStream bs, FloatCellCoordDecoder d, byte[] data, int offset) {
        PrimitiveType.FLOAT_VH.set(data, offset, bs.readCellCoord(d.nBits, d.integral, d.lowPrecision));
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.FLOAT;
    }

}
