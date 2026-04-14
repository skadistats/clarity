package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.state.PrimitiveType;

@RegisterDecoder
public final class IntVarSignedDecoder extends Decoder {

    public static Integer decode(BitStream bs) {
        return bs.readVarSInt();
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.INT;
    }

}
