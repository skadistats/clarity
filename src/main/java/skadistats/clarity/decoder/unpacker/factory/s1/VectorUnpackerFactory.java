package skadistats.clarity.decoder.unpacker.factory.s1;

import skadistats.clarity.decoder.s1.SendProp;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.decoder.unpacker.VectorDefaultUnpacker;
import skadistats.clarity.decoder.unpacker.VectorNormalUnpacker;
import skadistats.clarity.model.Vector;
import skadistats.clarity.model.s1.PropFlag;

public class VectorUnpackerFactory implements UnpackerFactory<Vector> {

    private final int dim;

    public VectorUnpackerFactory(int dim) {
        this.dim = dim;
    }

    public static Unpacker<Vector> createUnpackerStatic(int dim, SendProp prop) {
        if ((prop.getFlags() & PropFlag.NORMAL) != 0) {
            return new VectorNormalUnpacker();
        }
        return new VectorDefaultUnpacker(dim, FloatUnpackerFactory.createUnpackerStatic(prop));
    }

    @Override
    public Unpacker<Vector> createUnpacker(SendProp prop) {
        return createUnpackerStatic(dim, prop);
    }

}
