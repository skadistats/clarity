package clarity.decoder.dt;

import clarity.decoder.EntityBitStream;
import clarity.model.Prop;

public interface DtDecoder<T> {

	T decode(EntityBitStream stream, Prop prop);
	
}
