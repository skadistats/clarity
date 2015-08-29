package skadistats.clarity.decoder.unpacker.factory.s1;

import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.decoder.unpacker.VectorDefaultUnpacker;
import skadistats.clarity.decoder.unpacker.VectorNormalUnpacker;
import skadistats.clarity.model.Vector;
import skadistats.clarity.model.s1.PropFlag;
import skadistats.clarity.model.s1.SendProp;

public class VectorUnpackerFactory implements UnpackerFactory<Vector> {

    public static Unpacker<Vector> createUnpackerStatic(SendProp prop) {
        if ((prop.getFlags() & PropFlag.NORMAL) != 0) {
            return new VectorNormalUnpacker();
        }
        return new VectorDefaultUnpacker(FloatUnpackerFactory.createUnpackerStatic(prop));
    }

    @Override
    public Unpacker<Vector> createUnpacker(SendProp prop) {
        return createUnpackerStatic(prop);
    }
}
