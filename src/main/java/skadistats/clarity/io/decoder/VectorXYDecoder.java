package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Vector;
import skadistats.clarity.model.state.PrimitiveType;

@RegisterDecoder
public final class VectorXYDecoder extends Decoder {

    private final Decoder floatDecoder;

    public VectorXYDecoder(Decoder floatDecoder) {
        this.floatDecoder = floatDecoder;
    }

    public static Vector decode(BitStream bs, VectorXYDecoder d) {
        return new Vector(
            (Float) DecoderDispatch.decode(bs, d.floatDecoder),
            (Float) DecoderDispatch.decode(bs, d.floatDecoder)
        );
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 2);
    }

}
