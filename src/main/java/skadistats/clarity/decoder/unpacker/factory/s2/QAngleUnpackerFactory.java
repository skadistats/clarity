package skadistats.clarity.decoder.unpacker.factory.s2;

import skadistats.clarity.decoder.s2.field.FieldProperties;
import skadistats.clarity.decoder.unpacker.*;
import skadistats.clarity.model.Vector;

public class QAngleUnpackerFactory implements UnpackerFactory<Vector> {

    public static Unpacker<Vector> createUnpackerStatic(FieldProperties f) {
        int bc = f.getBitCountOrDefault(0);
        if ("qangle_pitch_yaw".equals(f.getEncoderType())) {
            return new QAnglePitchYawOnlyUnpacker(bc);
        }
        if (bc == 0) {
            return new QAngleNoBitCountUnpacker();
        }
        if (bc == 32) {
            return new QAngleNoScaleUnpacker();
        }
        return new QAngleBitCountUnpacker(bc);
    }

    @Override
    public Unpacker<Vector> createUnpacker(FieldProperties f) {
        return createUnpackerStatic(f);
    }

}
