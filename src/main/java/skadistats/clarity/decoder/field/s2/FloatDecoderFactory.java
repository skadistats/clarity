package skadistats.clarity.decoder.field.s2;

import skadistats.clarity.decoder.MH;
import skadistats.clarity.decoder.field.FloatDecoder;
import skadistats.clarity.model.s2.Field;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.util.HashSet;
import java.util.Set;

public class FloatDecoderFactory {

    private static final Set<String> SIMULATION_TIME_PROPERTIES = new HashSet<>();
    static {
        SIMULATION_TIME_PROPERTIES.add("m_flSimulationTime");
    }

    public static final MethodHandle createDecoder = MH.handle(
        FloatDecoderFactory.class, "createDecoder", MethodHandle.class, Field.class
    );

    public static MethodHandle createDecoder(Field f) {
        if ("coord".equals(f.getEncoder())) {
            return FloatDecoder.decodeCoord;
        }
        if (SIMULATION_TIME_PROPERTIES.contains(f.getName())) {
            return FloatDecoder.decodeSimulationTime;
        }
        int bc = f.getBitCount();
        if (bc <= 0 || bc >= 32) {
            return FloatDecoder.decodeNoScale;
        }
        return MethodHandles.insertArguments(FloatDecoder.decodeDefault, 1, bc, f.getLowValue(), f.getHighValue(), f.getEncodeFlags());
    }

}
