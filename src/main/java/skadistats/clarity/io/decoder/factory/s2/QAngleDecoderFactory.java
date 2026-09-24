package skadistats.clarity.io.decoder.factory.s2;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.QAngleBitCountDecoder;
import skadistats.clarity.io.decoder.QAngleNoBitCountDecoder;
import skadistats.clarity.io.decoder.QAngleNoScaleDecoder;
import skadistats.clarity.io.decoder.QAnglePitchYawOnlyDecoder;
import skadistats.clarity.io.decoder.QAnglePreciseDecoder;
import skadistats.clarity.model.s2.SerializerProperties;

public class QAngleDecoderFactory {

    public static Decoder createDecoder(SerializerProperties f) {
        var bc = f.getBitCountOrDefault(0);
        if ("qangle_pitch_yaw".equals(f.getEncoderType())) {
            return new QAnglePitchYawOnlyDecoder(bc);
        }
        // A bit count of 32 means raw floats and takes precedence over the encoder name:
        // CS2 declares CBodyComponentBaseModelEntity.m_angRotation as qangle_precise with
        // 32 bits, and the server writes it as three full floats.
        if (bc == 32) {
            return new QAngleNoScaleDecoder();
        }
        if ("qangle_precise".equals(f.getEncoderType())) {
            return new QAnglePreciseDecoder();
        }
        if (bc == 0) {
            return new QAngleNoBitCountDecoder();
        }
        return new QAngleBitCountDecoder(bc);
    }

}
