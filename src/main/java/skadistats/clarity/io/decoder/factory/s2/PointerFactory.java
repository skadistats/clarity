package skadistats.clarity.io.decoder.factory.s2;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.PointerDecoder;
import skadistats.clarity.io.s2.SerializerProperties;

public class PointerFactory {

    public static Decoder createDecoder(SerializerProperties f) {
        return new PointerDecoder(f.getPolymorphicTypes());
    }

}
