package skadistats.clarity.decoder.unpacker.factory.s2;

import skadistats.clarity.decoder.s2.field.FieldProperties;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.decoder.unpacker.VectorXYUnpacker;
import skadistats.clarity.model.Vector;

public class Vector2DUnpackerFactory implements UnpackerFactory<Vector> {

    public static Unpacker<Vector> createUnpackerStatic(FieldProperties f) {
        return new VectorXYUnpacker(FloatUnpackerFactory.createUnpackerStatic(f));
    }

    @Override
    public Unpacker<Vector> createUnpacker(FieldProperties f) {
        return createUnpackerStatic(f);
    }

}
