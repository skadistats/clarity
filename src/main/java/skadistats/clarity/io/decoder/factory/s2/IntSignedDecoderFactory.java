package skadistats.clarity.io.decoder.factory.s2;

import skadistats.clarity.io.s2.DecoderProperties;
import skadistats.clarity.io.decoder.IntSignedDecoder;
import skadistats.clarity.io.decoder.IntVarSignedDecoder;
import skadistats.clarity.io.decoder.Decoder;

public class IntSignedDecoderFactory implements DecoderFactory<Integer> {

    public static Decoder<Integer> createDecoderStatic(DecoderProperties f) {
        if ("fixed8".equals(f.getEncoderType())) {
            return new IntSignedDecoder(8);
        }
        return new IntVarSignedDecoder();
    }

    @Override
    public Decoder<Integer> createDecoder(DecoderProperties f) {
        return createDecoderStatic(f);
    }

}
