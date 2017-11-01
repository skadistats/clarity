package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.model.Vector;

public class VectorDefaultUnpacker implements Unpacker<Vector> {

    private final int dim;
    private final Unpacker<Float> floatUnpacker;

    public VectorDefaultUnpacker(int dim, Unpacker<Float> floatUnpacker) {
        this.dim = dim;
        this.floatUnpacker = floatUnpacker;
    }

    @Override
    public Vector unpack(BitStream bs) {
        float[] result = new float[dim];
        for (int i = 0; i < dim; i++) {
            result[i] = floatUnpacker.unpack(bs);
        }
        return new Vector(result);
    }
    
}
