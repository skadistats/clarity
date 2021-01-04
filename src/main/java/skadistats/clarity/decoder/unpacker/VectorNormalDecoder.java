package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.model.Vector;

public class VectorNormalDecoder implements Decoder<Vector> {

    @Override
    public Vector decode(BitStream bs) {
        return new Vector(
            bs.read3BitNormal()
        );
    }

}
