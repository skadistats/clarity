package clarity.decoder.prop;

import clarity.decoder.EntityBitStream;
import clarity.model.Prop;

public interface PropDecoder<T> {

    T decode(EntityBitStream stream, Prop prop);

}
