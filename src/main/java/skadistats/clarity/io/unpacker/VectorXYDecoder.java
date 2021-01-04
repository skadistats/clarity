package skadistats.clarity.io.unpacker;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Vector;

public class VectorXYDecoder implements Decoder<Vector> {

    private final Decoder<Float> floatDecoder;

    public VectorXYDecoder(Decoder<Float> floatDecoder) {
        this.floatDecoder = floatDecoder;
    }

    @Override
    public Vector decode(BitStream bs) {
        return new Vector(
            floatDecoder.decode(bs),
            floatDecoder.decode(bs)
        );
    }

}
