package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Vector;
import skadistats.clarity.model.state.PrimitiveType;

@RegisterDecoder
public final class QAngleBitCountDecoder extends Decoder {

    private final int nBits;

    public QAngleBitCountDecoder(int nBits) {
        this.nBits = nBits;
    }

    public static Vector decode(BitStream bs, QAngleBitCountDecoder d) {
        var v = new float[3];
        v[0] = bs.readBitAngle(d.nBits);
        v[1] = bs.readBitAngle(d.nBits);
        v[2] = bs.readBitAngle(d.nBits);
        return new Vector(v);
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 3);
    }

}
