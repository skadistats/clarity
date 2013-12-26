package clarity.decoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import clarity.decoder.prop.PropDecoder;
import clarity.model.ReceiveProp;

public class BaseInstanceDecoder {
	
	public static Map<Integer, Object> decode(byte[] data, List<ReceiveProp> receiveProps) {
		EntityBitStream stream = new EntityBitStream(data);
		List<Integer> propList = stream.readEntityPropList();

		Map<Integer, Object> state = new HashMap<Integer, Object>();
		for (int i : propList) {
			ReceiveProp p = receiveProps.get(i);
			PropDecoder<?> d = p.getType().getDecoder();
			Object value = d.decode(stream, p);
			state.put(i, value);
		}
		
		return state;
	}

}
