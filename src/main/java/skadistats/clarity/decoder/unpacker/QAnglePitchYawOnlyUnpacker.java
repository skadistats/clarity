package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.model.Vector;

public class QAnglePitchYawOnlyUnpacker implements Unpacker<Vector> {

    private final int nBits;

    public QAnglePitchYawOnlyUnpacker(int nBits) {
        this.nBits = nBits;
    }

    @Override
    public Vector unpack(BitStream bs) {
        float[] v = new float[3];
        if ((nBits | 0x20) == 0x20) {
            v[0] = Float.intBitsToFloat(bs.readUBitInt(32));
            v[1] = Float.intBitsToFloat(bs.readUBitInt(32));
        } else {
            v[0] = bs.readBitAngle(nBits);
            v[1] = bs.readBitAngle(nBits);
        }
        return new Vector(v);
    }
    
}
