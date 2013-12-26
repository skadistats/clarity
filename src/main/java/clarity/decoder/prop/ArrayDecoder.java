package clarity.decoder.prop;

import java.util.ArrayList;
import java.util.List;

import clarity.decoder.EntityBitStream;
import clarity.decoder.Util;
import clarity.model.Prop;

public class ArrayDecoder implements PropDecoder<List<Object>> {

	@Override
	public List<Object> decode(EntityBitStream stream, Prop prop) {
		int count = stream.readNumericBits(Util.calcBitsNeededFor(prop.getNumElements() - 1));
		List<Object> result = new ArrayList<Object>(count);
		PropDecoder<?> decoder = prop.getTemplate().getType().getDecoder();
		while (count > 0) {
			count--;
			Object x = decoder.decode(stream, prop.getTemplate());
			result.add(x);
		}
		return result;
	}

}
