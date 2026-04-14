package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Vector;
import skadistats.clarity.model.state.PrimitiveType;

@RegisterDecoder
public final class VectorNormalDecoder extends Decoder {

    public static Vector decode(BitStream bs) {
        return new Vector(
            bs.read3BitNormal()
        );
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return new PrimitiveType.VectorType(PrimitiveType.Scalar.FLOAT, 3);
    }

}
