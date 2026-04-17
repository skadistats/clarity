package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.state.PrimitiveType;

@RegisterDecoder
public final class BoolDecoder extends Decoder {

    public static Boolean decode(BitStream bs) {
        return bs.readBitFlag();
    }

    public static void decodeInto(BitStream bs, byte[] data, int offset) {
        data[offset] = bs.readBitFlag() ? (byte) 1 : (byte) 0;
    }

    @Override
    public PrimitiveType getPrimitiveType() {
        return PrimitiveType.Scalar.BOOL;
    }

}
