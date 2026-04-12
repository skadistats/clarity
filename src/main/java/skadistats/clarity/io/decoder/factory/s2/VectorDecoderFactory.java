package skadistats.clarity.io.decoder.factory.s2;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.VectorDefaultDecoder;
import skadistats.clarity.io.decoder.VectorNormalDecoder;
import skadistats.clarity.io.s2.SerializerProperties;

public class VectorDecoderFactory {

    public static Decoder createDecoder(int dim, SerializerProperties f) {
        if (dim == 3 && "normal".equals(f.getEncoderType())) {
            return new VectorNormalDecoder();
        }
        return new VectorDefaultDecoder(dim, FloatDecoderFactory.createDecoder(f));
    }

}
