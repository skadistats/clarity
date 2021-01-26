package skadistats.clarity.io.decoder.factory.s1;

import skadistats.clarity.io.s1.SendProp;
import skadistats.clarity.io.decoder.Decoder;

public interface DecoderFactory<T> {

    Decoder<T> createDecoder(SendProp prop);

}
