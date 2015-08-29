package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.Vector;

public class QAnglePitchYawOnlyUnpacker implements Unpacker<Vector> {

    private final int nBits;

    public QAnglePitchYawOnlyUnpacker(int nBits) {
        this.nBits = nBits;
    }

    @Override
    public Vector unpack(BitStream bs) {
        if ((nBits | 0x20) == 0x20) {
            throw new RuntimeException("implement me!");
        }
        float[] v = new float[3];
        v[0] = bs.readBitAngle(nBits);
        v[1] = bs.readBitAngle(nBits);
        v[2] = 0.0f;
        return new Vector(v);
    }
    
}
