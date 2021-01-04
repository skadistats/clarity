package skadistats.clarity.decoder.unpacker.factory.s2;

import skadistats.clarity.decoder.s2.field.DecoderProperties;
import skadistats.clarity.decoder.unpacker.LongUnsignedDecoder;
import skadistats.clarity.decoder.unpacker.LongVarUnsignedDecoder;
import skadistats.clarity.decoder.unpacker.Decoder;

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
