package skadistats.clarity.decoder.prop;

import javax.vecmath.Vector2f;

import skadistats.clarity.decoder.EntityBitStream;
import skadistats.clarity.model.Prop;
import skadistats.clarity.model.PropType;

public class VectorXYDecoder implements PropDecoder<Vector2f> {

    @Override
    public Vector2f decode(EntityBitStream stream, Prop prop) {
        FloatDecoder fd = (FloatDecoder) PropType.FLOAT.getDecoder();
        float x = fd.decode(stream, prop);
        float y = fd.decode(stream, prop);
        return new Vector2f(x, y);
    }

}
