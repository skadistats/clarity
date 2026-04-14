package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Vector;
import skadistats.clarity.model.state.PrimitiveType;

@RegisterDecoder
public final class VectorDecoder extends Decoder {

    private final Decoder floatDecoder;
    private final boolean normal;

    public VectorDecoder(Decoder floatDecoder, boolean normal) {
        this.floatDecoder = floatDecoder;
        this.normal = normal;
    }

    public static Vector decode(BitStream bs, VectorDecoder d) {
        var v = new float[3];
        v[0] = (Float) DecoderDispatch.decode(bs, d.floatDecoder);
        v[1] = (Float) DecoderDispatch.decode(bs, d.floatDecoder);
        if (!d.normal) {
            v[2] = (Float) DecoderDispatch.decode(bs, d.floatDecoder);
        } else {
            var s = bs.readBitFlag();
            var p = v[0] * v[0] + v[1] * v[1];
            if (p < 1.0f) v[2] = (float) Math.sqrt(1.0f - p);
            if (s) v[2] = -v[2];
        }
        return new Vector(v);
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 3);
    }

}
