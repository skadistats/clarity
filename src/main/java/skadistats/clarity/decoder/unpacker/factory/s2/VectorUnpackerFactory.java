package skadistats.clarity.decoder.unpacker.factory.s2;

import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.decoder.unpacker.VectorDefaultUnpacker;
import skadistats.clarity.decoder.unpacker.VectorNormalUnpacker;
import skadistats.clarity.model.Vector;
import skadistats.clarity.model.s2.field.FieldProperties;

public class VectorUnpackerFactory implements UnpackerFactory<Vector> {

    public static Unpacker<Vector> createUnpackerStatic(FieldProperties f) {
        if ("normal".equals(f.getEncoder())) {
            return new VectorNormalUnpacker();
        }
        return new VectorDefaultUnpacker(FloatUnpackerFactory.createUnpackerStatic(f));
    }

    @Override
    public Unpacker<Vector> createUnpacker(FieldProperties f) {
        return createUnpackerStatic(f);
    }

}
