package skadistats.clarity.decoder.unpacker.factory.s2;

import skadistats.clarity.decoder.s2.field.DecoderProperties;
import skadistats.clarity.decoder.unpacker.Decoder;

public interface DecoderFactory<T> {

    Decoder<T> createDecoder(DecoderProperties f);

}
