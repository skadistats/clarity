package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.Vector;

public class QAngleNoScaleUnpacker implements Unpacker<Vector> {

    @Override
    public Vector unpack(BitStream bs) {
        float[] v = new float[3];
        v[0] = Float.intBitsToFloat(bs.readUBitInt(32));
        v[1] = Float.intBitsToFloat(bs.readUBitInt(32));
        v[2] = Float.intBitsToFloat(bs.readUBitInt(32));
        return new Vector(v);
    }
    
}
