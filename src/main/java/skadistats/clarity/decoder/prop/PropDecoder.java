package skadistats.clarity.decoder.prop;

import skadistats.clarity.decoder.EntityBitStream;
import skadistats.clarity.model.Prop;

public interface PropDecoder<T> {

    T decode(EntityBitStream stream, Prop prop);

}
