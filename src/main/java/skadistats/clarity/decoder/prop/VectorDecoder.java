package skadistats.clarity.decoder.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.Prop;
import skadistats.clarity.model.PropFlag;
import skadistats.clarity.model.PropType;
import skadistats.clarity.model.Vector;

public class VectorDecoder implements PropDecoder<Vector> {

    @Override
    public Vector decode(BitStream stream, Prop prop) {
        FloatDecoder fd = (FloatDecoder) PropType.FLOAT.getDecoder();
        float x = fd.decode(stream, prop);
        float y = fd.decode(stream, prop);
        float z = 0.0f;
        if ((prop.getFlags() & PropFlag.NORMAL) != 0) {
            float f = x * x + y * y;
            z = 1.0f >= f ? 0.0f : (float) Math.sqrt(1.0f - f);
            z = stream.readBit() ? -z : z;
        } else {
            z = fd.decode(stream, prop);
        }
        return new Vector(x, y, z);
    }

}
