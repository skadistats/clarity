package skadistats.clarity.decoder.unpacker.factory.s2;

import skadistats.clarity.decoder.s2.field.FieldProperties;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.decoder.unpacker.VectorDefaultUnpacker;
import skadistats.clarity.decoder.unpacker.VectorNormalUnpacker;
import skadistats.clarity.model.Vector;

public class VectorUnpackerFactory implements UnpackerFactory<Vector> {

    private final int dim;

    public VectorUnpackerFactory(int dim) {
        this.dim = dim;
    }

    public static Unpacker<Vector> createUnpackerStatic(int dim, FieldProperties f) {
        if (dim == 3 && "normal".equals(f.getEncoderType())) {
            return new VectorNormalUnpacker();
        }
        return new VectorDefaultUnpacker(dim, FloatUnpackerFactory.createUnpackerStatic(f));
    }

    @Override
    public Unpacker<Vector> createUnpacker(FieldProperties f) {
        return createUnpackerStatic(dim, f);
    }

}
