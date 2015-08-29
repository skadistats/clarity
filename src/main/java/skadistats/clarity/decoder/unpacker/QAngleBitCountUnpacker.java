package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.Vector;

public class QAngleBitCountUnpacker implements Unpacker<Vector> {

    private final int nBits;

    public QAngleBitCountUnpacker(int nBits) {
        this.nBits = nBits;
    }

    @Override
    public Vector unpack(BitStream bs) {
        float[] v = new float[3];
        v[0] = bs.readBitAngle(nBits);
        v[1] = bs.readBitAngle(nBits);
        v[2] = bs.readBitAngle(nBits);
        return new Vector(v);
    }
    
}
