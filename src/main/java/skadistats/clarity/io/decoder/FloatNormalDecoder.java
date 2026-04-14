package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.state.PrimitiveType;

@RegisterDecoder
public final class FloatNormalDecoder extends Decoder {

    public static Float decode(BitStream bs) {
        return bs.readBitNormal();
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.FLOAT;
    }

}
