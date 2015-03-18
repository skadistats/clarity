package skadistats.clarity.decoder.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.Prop;

public interface PropDecoder<T> {

    T decode(BitStream stream, Prop prop);

}
