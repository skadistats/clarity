package skadistats.clarity.io.decoder.factory.s2;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.QAngleBitCountDecoder;
import skadistats.clarity.io.decoder.QAngleNoBitCountDecoder;
import skadistats.clarity.io.decoder.QAngleNoScaleDecoder;
import skadistats.clarity.io.decoder.QAnglePitchYawOnlyDecoder;
import skadistats.clarity.io.decoder.QAnglePreciseDecoder;
import skadistats.clarity.io.s2.SerializerProperties;

public class QAngleDecoderFactory {

    public static Decoder createDecoder(SerializerProperties f) {
        var bc = f.getBitCountOrDefault(0);
        if ("qangle_pitch_yaw".equals(f.getEncoderType())) {
            return new QAnglePitchYawOnlyDecoder(bc);
        }
        if ("qangle_precise".equals(f.getEncoderType())) {
            return new QAnglePreciseDecoder();
        }
        if (bc == 0) {
            return new QAngleNoBitCountDecoder();
        }
        if (bc == 32) {
            return new QAngleNoScaleDecoder();
        }
        return new QAngleBitCountDecoder(bc);
    }

}
