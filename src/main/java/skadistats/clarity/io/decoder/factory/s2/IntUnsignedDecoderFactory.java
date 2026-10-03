package skadistats.clarity.io.decoder.factory.s2;

import skadistats.clarity.io.s2.DecoderProperties;
import skadistats.clarity.io.decoder.IntUnsignedDecoder;
import skadistats.clarity.io.decoder.IntVarUnsignedDecoder;
import skadistats.clarity.io.decoder.Decoder;

public class IntUnsignedDecoderFactory implements DecoderFactory<Integer> {

    public static Decoder<Integer> createDecoderStatic(DecoderProperties f) {
        if ("fixed8".equals(f.getEncoderType())) {
            return new IntUnsignedDecoder(8);
        }
        return new IntVarUnsignedDecoder();
    }

    @Override
    public Decoder<Integer> createDecoder(DecoderProperties f) {
        return createDecoderStatic(f);
    }

}
