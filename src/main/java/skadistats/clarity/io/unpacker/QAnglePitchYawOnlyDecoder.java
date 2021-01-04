package skadistats.clarity.io.unpacker;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Vector;

public class QAnglePitchYawOnlyDecoder implements Decoder<Vector> {

    private final int nBits;

    public QAnglePitchYawOnlyDecoder(int nBits) {
        this.nBits = nBits;
    }

    @Override
    public Vector decode(BitStream bs) {
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
