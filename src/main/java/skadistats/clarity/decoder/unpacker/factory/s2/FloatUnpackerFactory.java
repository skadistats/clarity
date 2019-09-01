package skadistats.clarity.decoder.unpacker.factory.s2;

import skadistats.clarity.decoder.s2.field.UnpackerProperties;
import skadistats.clarity.decoder.unpacker.FloatCoordUnpacker;
import skadistats.clarity.decoder.unpacker.FloatNoScaleUnpacker;
import skadistats.clarity.decoder.unpacker.FloatQuantizedUnpacker;
import skadistats.clarity.decoder.unpacker.FloatSimulationTimeUnpacker;
import skadistats.clarity.decoder.unpacker.Unpacker;

public class FloatUnpackerFactory implements UnpackerFactory<Float> {

    @Override
    public Unpacker<Float> createUnpacker(UnpackerProperties f) {
        return createUnpackerStatic(f);
    }

    public static Unpacker<Float> createUnpackerStatic(UnpackerProperties f) {
        if ("coord".equals(f.getEncoderType())) {
            return new FloatCoordUnpacker();
        }
        if ("simulationtime".equals(f.getSerializerType())) {
            return new FloatSimulationTimeUnpacker();
        }
        int bc = f.getBitCountOrDefault(0);
        if (bc <= 0 || bc >= 32) {
            return new FloatNoScaleUnpacker();
        }
        // TODO: get real name
        return new FloatQuantizedUnpacker("N/A", bc, f.getEncodeFlagsOrDefault(0) & 0xF, f.getLowValueOrDefault(0.0f), f.getHighValueOrDefault(1.0f));
    }

}
