package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Vector;

public class QAngleBitCountDecoder implements Decoder<Vector> {

    private final int nBits;

    public QAngleBitCountDecoder(int nBits) {
        this.nBits = nBits;
    }

    @Override
    public Vector decode(BitStream bs) {
        var v = new float[3];
        v[0] = bs.readBitAngle(nBits);
        v[1] = bs.readBitAngle(nBits);
        v[2] = bs.readBitAngle(nBits);
        return new Vector(v);
    }

}
