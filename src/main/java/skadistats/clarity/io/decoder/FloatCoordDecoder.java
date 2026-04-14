package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.state.PrimitiveType;

@RegisterDecoder
public final class FloatCoordDecoder extends Decoder {

    public static Float decode(BitStream bs) {
        return bs.readBitCoord();
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.FLOAT;
    }

}
