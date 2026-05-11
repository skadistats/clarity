package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Vector;
import skadistats.clarity.state.PrimitiveType;

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

    public static void decodeInto(BitStream bs, VectorDecoder d, byte[] data, int offset) {
        DecoderDispatch.decodeInto(bs, d.floatDecoder, data, offset);
        DecoderDispatch.decodeInto(bs, d.floatDecoder, data, offset + 4);
        if (!d.normal) {
            DecoderDispatch.decodeInto(bs, d.floatDecoder, data, offset + 8);
        } else {
            var s = bs.readBitFlag();
            var x = (float) PrimitiveType.FLOAT_VH.get(data, offset);
            var y = (float) PrimitiveType.FLOAT_VH.get(data, offset + 4);
            var p = x * x + y * y;
            float z = 0f;
            if (p < 1.0f) z = (float) Math.sqrt(1.0f - p);
            if (s) z = -z;
            PrimitiveType.FLOAT_VH.set(data, offset + 8, z);
        }
    }

    public static void skip(BitStream bs, VectorDecoder d) {
        DecoderDispatch.skip(bs, d.floatDecoder);
        DecoderDispatch.skip(bs, d.floatDecoder);
        if (d.normal) {
            bs.skip(1);
        } else {
            DecoderDispatch.skip(bs, d.floatDecoder);
        }
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 3);
    }

}
