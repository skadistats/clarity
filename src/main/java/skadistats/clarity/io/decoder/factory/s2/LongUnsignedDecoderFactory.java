package skadistats.clarity.io.decoder.factory.s2;

import skadistats.clarity.io.s2.field.DecoderProperties;
import skadistats.clarity.io.decoder.LongUnsignedDecoder;
import skadistats.clarity.io.decoder.LongVarUnsignedDecoder;
import skadistats.clarity.io.decoder.Decoder;

public class LongUnsignedDecoderFactory implements DecoderFactory<Long> {

    public static Decoder<Long> createDecoderStatic(DecoderProperties f) {
        if ("fixed64".equals(f.getEncoderType())) {
            return new LongUnsignedDecoder(64);
        }
        return new LongVarUnsignedDecoder();
    }

    @Override
    public Decoder<Long> createDecoder(DecoderProperties f) {
        return createDecoderStatic(f);
    }

}
