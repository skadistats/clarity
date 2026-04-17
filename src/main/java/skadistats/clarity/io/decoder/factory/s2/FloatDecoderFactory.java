package skadistats.clarity.io.decoder.factory.s2;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.FloatCoordDecoder;
import skadistats.clarity.io.decoder.FloatNoScaleDecoder;
import skadistats.clarity.io.decoder.FloatQuantizedDecoder;
import skadistats.clarity.model.s2.SerializerProperties;

public class FloatDecoderFactory {

    public static Decoder createDecoder(SerializerProperties f) {
        if ("coord".equals(f.getEncoderType())) {
            return new FloatCoordDecoder();
        }
        var bc = f.getBitCountOrDefault(0);
        if (bc <= 0 || bc >= 32) {
            return new FloatNoScaleDecoder();
        }
        // TODO: get real name
        return new FloatQuantizedDecoder("N/A", bc, f.getEncodeFlagsOrDefault(0) & 0xF, f.getLowValueOrDefault(0.0f), f.getHighValueOrDefault(1.0f));
    }

}
