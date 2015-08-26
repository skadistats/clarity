package skadistats.clarity.decoder.field.s2;

import skadistats.clarity.decoder.MH;
import skadistats.clarity.decoder.field.QAngleDecoder;
import skadistats.clarity.model.s2.Field;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public class QAngleDecoderFactory {

    public static final MethodHandle createDecoder = MH.handle(
        QAngleDecoderFactory.class, "createDecoder", MethodHandle.class, Field.class
    );

    public static MethodHandle createDecoder(Field f) {
        int bc = f.getBitCount();
        if ("qangle_pitch_yaw".equals(f.getEncoder())) {
            return MethodHandles.insertArguments(QAngleDecoder.decodePitchYawOnly, 1, bc);
        }
        if (bc == 0) {
            return QAngleDecoder.decodeDefaultNoBitCount;
        }
        if (bc == 32) {
            return QAngleDecoder.decodeDefaultNoScale;
        }
        return MethodHandles.insertArguments(QAngleDecoder.decodeDefaultBitCount, 1, bc);
    }

}
