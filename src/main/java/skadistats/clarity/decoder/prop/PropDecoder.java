package skadistats.clarity.decoder.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.SendProp;

public interface PropDecoder<T> {

    T decode(BitStream stream, SendProp prop);

}
