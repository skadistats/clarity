package skadistats.clarity.decoder.unpacker.factory.s2;

import skadistats.clarity.decoder.s2.field.FieldProperties;
import skadistats.clarity.decoder.unpacker.*;

public class FloatUnpackerFactory implements UnpackerFactory<Float> {

    @Override
    public Unpacker<Float> createUnpacker(FieldProperties f) {
        return createUnpackerStatic(f);
    }

    public static Unpacker<Float> createUnpackerStatic(FieldProperties f) {
        if ("coord".equals(f.getEncoderType())) {
            return new FloatCoordUnpacker();
        }
        if ("simulationtime".equals(f.getSerializerType())) {
            return new FloatSimulationTimeUnpacker();
        }
	if ("runetime".equals(f.getSerializerType())) {
	    return new FloatRuneTimeUnpacker();
	}
        int bc = f.getBitCountOrDefault(0);
        if (bc <= 0 || bc >= 32) {
            return new FloatNoScaleUnpacker();
        }
        return new FloatQuantizedUnpacker(f.getName(), bc, f.getEncodeFlagsOrDefault(0) & 0xF, f.getLowValueOrDefault(0.0f), f.getHighValueOrDefault(1.0f));
    }

}
