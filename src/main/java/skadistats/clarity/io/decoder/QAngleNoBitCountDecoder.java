package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Vector;

public class QAngleNoBitCountDecoder implements Decoder<Vector> {

    @Override
    public Vector decode(BitStream bs) {
        var v = new float[3];
        var b0 = bs.readBitFlag();
        var b1 = bs.readBitFlag();
        var b2 = bs.readBitFlag();
        if (b0) v[0] = bs.readBitCoord();
        if (b1) v[1] = bs.readBitCoord();
        if (b2) v[2] = bs.readBitCoord();
        return new Vector(v);
    }

}
