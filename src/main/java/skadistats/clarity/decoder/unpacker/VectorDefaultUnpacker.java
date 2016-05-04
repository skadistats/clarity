package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.bitstream.BitStream;
import skadistats.clarity.model.Vector;

public class VectorDefaultUnpacker implements Unpacker<Vector> {

    private final Unpacker<Float> floatUnpacker;

    public VectorDefaultUnpacker(Unpacker<Float> floatUnpacker) {
        this.floatUnpacker = floatUnpacker;
    }

    @Override
    public Vector unpack(BitStream bs) {
        return new Vector(
            floatUnpacker.unpack(bs),
            floatUnpacker.unpack(bs),
            floatUnpacker.unpack(bs)
        );
    }
    
}
