package skadistats.clarity.decoder.s1.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.Vector;
import skadistats.clarity.model.s1.PropType;
import skadistats.clarity.model.s1.SendProp;

public class VectorXYDecoder implements PropDecoder<Vector> {

    @Override
    public Vector decode(BitStream stream, SendProp prop) {
        FloatDecoder fd = (FloatDecoder) PropType.FLOAT.getDecoder();
        return new Vector(fd.decode(stream, prop), fd.decode(stream, prop));
    }

}
