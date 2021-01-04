package skadistats.clarity.io.unpacker;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Vector;

public class VectorNormalDecoder implements Decoder<Vector> {

    @Override
    public Vector decode(BitStream bs) {
        return new Vector(
            bs.read3BitNormal()
        );
    }

}
