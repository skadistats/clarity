package skadistats.clarity.io.unpacker.factory.s2;

import skadistats.clarity.io.s2.field.DecoderProperties;
import skadistats.clarity.io.unpacker.Decoder;
import skadistats.clarity.io.unpacker.VectorDefaultDecoder;
import skadistats.clarity.io.unpacker.VectorNormalDecoder;
import skadistats.clarity.model.Vector;

public class VectorDecoderFactory implements DecoderFactory<Vector> {

    private final int dim;

    public VectorDecoderFactory(int dim) {
        this.dim = dim;
    }

    public static Decoder<Vector> createDecoderStatic(int dim, DecoderProperties f) {
        if (dim == 3 && "normal".equals(f.getEncoderType())) {
            return new VectorNormalDecoder();
        }
        return new VectorDefaultDecoder(dim, FloatDecoderFactory.createDecoderStatic(f));
    }

    @Override
    public Decoder<Vector> createDecoder(DecoderProperties f) {
        return createDecoderStatic(dim, f);
    }

}
