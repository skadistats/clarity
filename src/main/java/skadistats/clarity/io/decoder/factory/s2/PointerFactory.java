package skadistats.clarity.io.decoder.factory.s2;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.PointerDecoder;
import skadistats.clarity.io.s2.DecoderProperties;
import skadistats.clarity.io.s2.Pointer;

public class PointerFactory implements DecoderFactory<Pointer> {

    public static Decoder<Pointer> createDecoderStatic(DecoderProperties f) {
        return new PointerDecoder(f.getPolymorphicTypes());
    }

    @Override
    public Decoder<Pointer> createDecoder(DecoderProperties f) {
        return createDecoderStatic(f);
    }

}
