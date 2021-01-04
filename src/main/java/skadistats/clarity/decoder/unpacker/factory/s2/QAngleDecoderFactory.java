package skadistats.clarity.decoder.unpacker.factory.s2;

import skadistats.clarity.decoder.s2.field.DecoderProperties;
import skadistats.clarity.decoder.unpacker.QAngleBitCountDecoder;
import skadistats.clarity.decoder.unpacker.QAngleNoBitCountDecoder;
import skadistats.clarity.decoder.unpacker.QAngleNoScaleDecoder;
import skadistats.clarity.decoder.unpacker.QAnglePitchYawOnlyDecoder;
import skadistats.clarity.decoder.unpacker.Decoder;
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
