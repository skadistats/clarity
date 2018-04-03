package skadistats.clarity.decoder.unpacker.factory.s1;

import skadistats.clarity.decoder.s1.SendProp;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.decoder.unpacker.VectorUnpacker;
import skadistats.clarity.decoder.unpacker.VectorXYUnpacker;
import skadistats.clarity.model.Vector;
import skadistats.clarity.model.s1.PropFlag;

public class VectorUnpackerFactory implements UnpackerFactory<Vector> {

    private final int dim;

    public VectorUnpackerFactory(int dim) {
        this.dim = dim;
    }

    public static Unpacker<Vector> createUnpackerStatic(int dim, SendProp prop) {
        if (dim == 3) {
            return new VectorUnpacker(FloatUnpackerFactory.createUnpackerStatic(prop), (prop.getFlags() & PropFlag.NORMAL) != 0);
        } else {
            return new VectorXYUnpacker(FloatUnpackerFactory.createUnpackerStatic(prop));
        }
    }

    @Override
    public Unpacker<Vector> createUnpacker(SendProp prop) {
        return createUnpackerStatic(dim, prop);
    }

}
