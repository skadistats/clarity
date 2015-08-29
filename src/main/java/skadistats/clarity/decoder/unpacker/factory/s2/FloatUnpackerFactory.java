package skadistats.clarity.decoder.unpacker.factory.s2;

import skadistats.clarity.decoder.unpacker.*;
import skadistats.clarity.model.s2.Field;

import java.util.HashSet;
import java.util.Set;

public class FloatUnpackerFactory implements UnpackerFactory<Float> {

    private static final Set<String> SIMULATION_TIME_PROPERTIES = new HashSet<>();
    static {
        SIMULATION_TIME_PROPERTIES.add("m_flSimulationTime");
        SIMULATION_TIME_PROPERTIES.add("m_flAnimTime");
    }

    @Override
    public Unpacker<Float> createUnpacker(Field f) {
        return createUnpackerStatic(f);
    }

    public static Unpacker<Float> createUnpackerStatic(Field f) {
        if ("coord".equals(f.getEncoder())) {
            return new FloatCoordUnpacker();
        }
        if (SIMULATION_TIME_PROPERTIES.contains(f.getName())) {
            return new FloatSimulationTimeUnpacker();
        }
        int bc = f.getBitCountOrDefault(0);
        if (bc <= 0 || bc >= 32) {
            return new FloatNoScaleUnpacker();
        }
        return new FloatQuantizedUnpacker(f.getName(), bc, f.getEncodeFlagsOrDefault(0) & 0xF, f.getLowValueOrDefault(0.0f), f.getHighValueOrDefault(1.0f));
    }



}
