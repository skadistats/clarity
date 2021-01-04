package skadistats.clarity.io.decoder.factory.s2;

import skadistats.clarity.io.s2.DecoderProperties;
import skadistats.clarity.io.decoder.QAngleBitCountDecoder;
import skadistats.clarity.io.decoder.QAngleNoBitCountDecoder;
import skadistats.clarity.io.decoder.QAngleNoScaleDecoder;
import skadistats.clarity.io.decoder.QAnglePitchYawOnlyDecoder;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.model.Vector;

public class QAngleDecoderFactory implements DecoderFactory<Vector> {

    public static Decoder<Vector> createDecoderStatic(DecoderProperties f) {
        int bc = f.getBitCountOrDefault(0);
        if ("qangle_pitch_yaw".equals(f.getEncoderType())) {
            return new QAnglePitchYawOnlyDecoder(bc);
        }
        if (bc == 0) {
            return new QAngleNoBitCountDecoder();
        }
        if (bc == 32) {
            return new QAngleNoScaleDecoder();
        }
        return new QAngleBitCountDecoder(bc);
    }

    @Override
    public Decoder<Vector> createDecoder(DecoderProperties f) {
        return createDecoderStatic(f);
    }

}
