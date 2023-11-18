package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Vector;

public class QAnglePreciseDecoder implements Decoder<Vector> {

    @Override
    public Vector decode(BitStream bs) {
        var v = new float[3];
        boolean hasX = bs.readBitFlag();
        boolean hasY = bs.readBitFlag();
        boolean hasZ = bs.readBitFlag();
        if (hasX) v[0] = bs.readBitAngle(20);
        if (hasY) v[1] = bs.readBitAngle(20);
        if (hasZ) v[2] = bs.readBitAngle(20);
        return new Vector(v);
    }

}
