package clarity.decoder.dt;

import org.javatuples.Triplet;

import clarity.decoder.EntityBitStream;
import clarity.model.Prop;
import clarity.model.PropFlag;
import clarity.model.PropType;

public class VectorDecoder implements DtDecoder<Triplet<Float, Float, Float>> {

	@Override
	public Triplet<Float, Float, Float> decode(EntityBitStream stream, Prop prop) {
		FloatDecoder fd = (FloatDecoder) PropType.FLOAT.getDecoder();
		float x = fd.decode(stream, prop);
		float y = fd.decode(stream, prop);
		float z = 0.0f;
		if (prop.isFlagSet(PropFlag.NORMAL)) {
            float f = x * x + y * y;
            z = 1.0f >= f ? 0.0f : (float)Math.sqrt(1.0f - f);
            z = stream.readBit() ? -z : z;
		} else {
            z = fd.decode(stream, prop);
		}
        return new Triplet<Float, Float, Float>(x, y, z);
	}

}
