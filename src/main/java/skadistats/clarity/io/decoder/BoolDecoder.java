package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.state.PrimitiveType;

@RegisterDecoder
public final class BoolDecoder extends Decoder {

    public static Boolean decode(BitStream bs) {
        return bs.readBitFlag();
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.BOOL;
    }

}
