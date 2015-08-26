package skadistats.clarity.decoder.field.s2;

import skadistats.clarity.decoder.MH;
import skadistats.clarity.decoder.field.VectorDecoder;
import skadistats.clarity.model.s2.Field;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

public class VectorDecoderFactory {

    public static final MethodHandle createDecoder = MH.handle(
        VectorDecoderFactory.class, "createDecoder", MethodHandle.class, Field.class
    );

    public static MethodHandle createDecoder(Field f) {
        if ("normal".equals(f.getEncoder())) {
            return VectorDecoder.decodeNormal;
        }
        return MethodHandles.insertArguments(VectorDecoder.decodeDefault, 1, FloatDecoderFactory.createDecoder(f));
    }

}
