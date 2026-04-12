package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Vector;

@RegisterDecoder
public final class VectorNormalDecoder extends Decoder {

    public static Vector decode(BitStream bs) {
        return new Vector(
            bs.read3BitNormal()
        );
    }

}
