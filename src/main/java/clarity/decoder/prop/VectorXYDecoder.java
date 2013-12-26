package clarity.decoder.prop;

import org.javatuples.Pair;

import clarity.decoder.EntityBitStream;
import clarity.model.Prop;
import clarity.model.PropType;

public class VectorXYDecoder implements PropDecoder<Pair<Float, Float>> {

	@Override
	public Pair<Float, Float> decode(EntityBitStream stream, Prop prop) {
		FloatDecoder fd = (FloatDecoder) PropType.FLOAT.getDecoder();
		float x = fd.decode(stream, prop);
		float y = fd.decode(stream, prop);
		return new Pair<Float, Float>(x, y);
	}

}
