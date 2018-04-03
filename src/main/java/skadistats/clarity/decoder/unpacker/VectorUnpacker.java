package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.model.Vector;

public class VectorUnpacker implements Unpacker<Vector> {

    private final Unpacker<Float> floatUnpacker;
    private final boolean normal;


    public VectorUnpacker(Unpacker<Float> floatUnpacker, boolean normal) {
        this.floatUnpacker = floatUnpacker;
        this.normal = normal;
    }

    @Override
    public Vector unpack(BitStream bs) {
        float[] v = new float[3];
        v[0] = floatUnpacker.unpack(bs);
        v[1] = floatUnpacker.unpack(bs);
        if (!normal) {
            v[2] = floatUnpacker.unpack(bs);
        } else {
            boolean s = bs.readBitFlag();
            float p = v[0] * v[0] + v[1] * v[1];
            if (p < 1.0f) v[2] = (float) Math.sqrt(1.0f - p);
            if (s) v[2] = -v[2];
        }
        return new Vector(v);
    }

}
