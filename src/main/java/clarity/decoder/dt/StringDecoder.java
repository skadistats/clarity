package clarity.decoder.dt;

import clarity.decoder.EntityBitStream;
import clarity.model.Prop;

public class StringDecoder implements DtDecoder<String> {

	@Override
	public String decode(EntityBitStream stream, Prop prop) {
		int len = stream.readNumericBits(9);
		return stream.readString(len);
	}

}
