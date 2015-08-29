package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.Vector;

public class VectorXYUnpacker implements Unpacker<Vector> {

    private final Unpacker<Float> floatUnpacker;

    public VectorXYUnpacker(Unpacker<Float> floatUnpacker) {
        this.floatUnpacker = floatUnpacker;
    }

    @Override
    public Vector unpack(BitStream bs) {
        return new Vector(
            floatUnpacker.unpack(bs),
            floatUnpacker.unpack(bs)
        );
    }
    
}
