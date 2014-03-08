package skadistats.clarity.decoder.prop;

import javax.vecmath.Vector3f;

import skadistats.clarity.decoder.EntityBitStream;
import skadistats.clarity.model.Prop;
import skadistats.clarity.model.PropFlag;
import skadistats.clarity.model.PropType;

public class VectorDecoder implements PropDecoder<Vector3f> {

    @Override
    public Vector3f decode(EntityBitStream stream, Prop prop) {
        FloatDecoder fd = (FloatDecoder) PropType.FLOAT.getDecoder();
        float x = fd.decode(stream, prop);
        float y = fd.decode(stream, prop);
        float z = 0.0f;
        if (prop.isFlagSet(PropFlag.NORMAL)) {
            float f = x * x + y * y;
            z = 1.0f >= f ? 0.0f : (float) Math.sqrt(1.0f - f);
            z = stream.readBit() ? -z : z;
        } else {
            z = fd.decode(stream, prop);
        }
        return new Vector3f(x, y, z);
    }

}
