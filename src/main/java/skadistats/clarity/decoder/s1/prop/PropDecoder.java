package skadistats.clarity.decoder.s1.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.s1.SendProp;

public interface PropDecoder<T> {

    T decode(BitStream stream, SendProp prop);

}
