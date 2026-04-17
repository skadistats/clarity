package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Vector;
import skadistats.clarity.state.PrimitiveType;

@RegisterDecoder
public final class VectorDefaultDecoder extends Decoder {

    private final int dim;
    private final Decoder floatDecoder;

    public VectorDefaultDecoder(int dim, Decoder floatDecoder) {
        this.dim = dim;
        this.floatDecoder = floatDecoder;
    }

    public static Vector decode(BitStream bs, VectorDefaultDecoder d) {
        var result = new float[d.dim];
        for (var i = 0; i < d.dim; i++) {
            result[i] = (Float) DecoderDispatch.decode(bs, d.floatDecoder);
        }
        return new Vector(result);
    }

    public static void decodeInto(BitStream bs, VectorDefaultDecoder d, byte[] data, int offset) {
        for (var i = 0; i < d.dim; i++) {
            DecoderDispatch.decodeInto(bs, d.floatDecoder, data, offset + i * 4);
        }
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, dim);
    }

}
