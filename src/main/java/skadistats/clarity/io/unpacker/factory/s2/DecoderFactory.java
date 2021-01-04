package skadistats.clarity.io.unpacker.factory.s2;

import skadistats.clarity.io.s2.field.DecoderProperties;
import skadistats.clarity.io.unpacker.Decoder;

public interface DecoderFactory<T> {

    Decoder<T> createDecoder(DecoderProperties f);

}
