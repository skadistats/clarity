package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.model.Vector;

public class VectorNormalUnpacker implements Unpacker<Vector> {

    @Override
    public Vector unpack(BitStream bs) {
        return new Vector(
            bs.read3BitNormal()
        );
    }
    
}
