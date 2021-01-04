package skadistats.clarity.io.decoder.factory.s2;

import skadistats.clarity.io.s2.field.DecoderProperties;
import skadistats.clarity.io.decoder.Decoder;

public interface DecoderFactory<T> {

    Decoder<T> createDecoder(DecoderProperties f);

}
