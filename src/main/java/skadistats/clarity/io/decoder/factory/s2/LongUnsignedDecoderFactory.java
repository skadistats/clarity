package skadistats.clarity.io.decoder.factory.s2;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.LongUnsignedDecoder;
import skadistats.clarity.io.decoder.LongVarUnsignedDecoder;
import skadistats.clarity.io.s2.SerializerProperties;

public class LongUnsignedDecoderFactory {

    public static Decoder createDecoder(SerializerProperties f) {
        if ("fixed64".equals(f.getEncoderType())) {
            return new LongUnsignedDecoder(64);
        }
        return new LongVarUnsignedDecoder();
    }

}
