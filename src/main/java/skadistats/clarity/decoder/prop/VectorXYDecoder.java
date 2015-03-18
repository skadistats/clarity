package skadistats.clarity.decoder.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.Prop;
import skadistats.clarity.model.PropType;
import skadistats.clarity.model.Vector;

public class VectorXYDecoder implements PropDecoder<Vector> {

    @Override
    public Vector decode(BitStream stream, Prop prop) {
        FloatDecoder fd = (FloatDecoder) PropType.FLOAT.getDecoder();
        return new Vector(fd.decode(stream, prop), fd.decode(stream, prop));
    }

}
