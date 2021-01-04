package skadistats.clarity.io.unpacker.factory.s1;

import skadistats.clarity.io.s1.SendProp;
import skadistats.clarity.io.unpacker.Decoder;

public interface DecoderFactory<T> {

    Decoder<T> createDecoder(SendProp prop);

}
