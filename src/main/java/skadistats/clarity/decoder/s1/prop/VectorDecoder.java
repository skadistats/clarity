package skadistats.clarity.decoder.s1.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.Vector;
import skadistats.clarity.model.s1.PropFlag;
import skadistats.clarity.model.s1.PropType;
import skadistats.clarity.model.s1.SendProp;

public class VectorDecoder implements PropDecoder<Vector> {

    @Override
    public Vector decode(BitStream stream, SendProp prop) {
        FloatDecoder fd = (FloatDecoder) PropType.FLOAT.getDecoder();
        float x = fd.decode(stream, prop);
        float y = fd.decode(stream, prop);
        float z = 0.0f;
        if ((prop.getFlags() & PropFlag.NORMAL) != 0) {
            float f = x * x + y * y;
            z = 1.0f >= f ? 0.0f : (float) Math.sqrt(1.0f - f);
            z = stream.readBits(1) == 1 ? -z : z;
        } else {
            z = fd.decode(stream, prop);
        }
        return new Vector(x, y, z);
    }

}
