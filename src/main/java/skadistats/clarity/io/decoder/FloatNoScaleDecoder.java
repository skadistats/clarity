package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.state.PrimitiveType;

@RegisterDecoder
public final class FloatNoScaleDecoder extends Decoder {

    public static Float decode(BitStream bs) {
        return Float.intBitsToFloat(bs.readUBitInt(32));
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.FLOAT;
    }

}
