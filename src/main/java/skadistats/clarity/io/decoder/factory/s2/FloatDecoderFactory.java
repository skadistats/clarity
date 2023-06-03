package skadistats.clarity.io.decoder.factory.s2;

import skadistats.clarity.io.s2.DecoderProperties;
import skadistats.clarity.io.decoder.FloatCoordDecoder;
import skadistats.clarity.io.decoder.FloatNoScaleDecoder;
import skadistats.clarity.io.decoder.FloatQuantizedDecoder;
import skadistats.clarity.io.decoder.FloatSimulationTimeDecoder;
import skadistats.clarity.io.decoder.Decoder;

public class FloatDecoderFactory implements DecoderFactory<Float> {

    @Override
    public Decoder<Float> createDecoder(DecoderProperties f) {
        return createDecoderStatic(f);
    }

    public static Decoder<Float> createDecoderStatic(DecoderProperties f) {
        if ("coord".equals(f.getEncoderType())) {
            return new FloatCoordDecoder();
        }
        if ("simulationtime".equals(f.getEncoderType())) {
            return new FloatSimulationTimeDecoder();
        }
        var bc = f.getBitCountOrDefault(0);
        if (bc <= 0 || bc >= 32) {
            return new FloatNoScaleDecoder();
        }
        // TODO: get real name
        return new FloatQuantizedDecoder("N/A", bc, f.getEncodeFlagsOrDefault(0) & 0xF, f.getLowValueOrDefault(0.0f), f.getHighValueOrDefault(1.0f));
    }

}
