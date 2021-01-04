package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Vector;

public class QAngleNoBitCountDecoder implements Decoder<Vector> {

    @Override
    public Vector decode(BitStream bs) {
        float[] v = new float[3];
        boolean b0 = bs.readBitFlag();
        boolean b1 = bs.readBitFlag();
        boolean b2 = bs.readBitFlag();
        if (b0) v[0] = bs.readBitCoord();
        if (b1) v[1] = bs.readBitCoord();
        if (b2) v[2] = bs.readBitCoord();
        return new Vector(v);
    }

}
