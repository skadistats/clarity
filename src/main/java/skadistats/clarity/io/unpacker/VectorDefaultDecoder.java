package skadistats.clarity.io.unpacker;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Vector;

public class VectorDefaultDecoder implements Decoder<Vector> {

    private final int dim;
    private final Decoder<Float> floatDecoder;

    public VectorDefaultDecoder(int dim, Decoder<Float> floatDecoder) {
        this.dim = dim;
        this.floatDecoder = floatDecoder;
    }

    @Override
    public Vector decode(BitStream bs) {
        float[] result = new float[dim];
        for (int i = 0; i < dim; i++) {
            result[i] = floatDecoder.decode(bs);
        }
        return new Vector(result);
    }

}
