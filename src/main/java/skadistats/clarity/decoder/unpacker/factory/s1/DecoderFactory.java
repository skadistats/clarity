package skadistats.clarity.decoder.unpacker.factory.s1;

import skadistats.clarity.decoder.s1.SendProp;
import skadistats.clarity.decoder.unpacker.Decoder;

public interface DecoderFactory<T> {

    Decoder<T> createDecoder(SendProp prop);

}
