package skadistats.clarity.decoder.unpacker.factory.s1;

import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.decoder.unpacker.VectorXYUnpacker;
import skadistats.clarity.model.Vector;
import skadistats.clarity.model.s1.SendProp;

public class VectorXYUnpackerFactory implements UnpackerFactory<Vector> {

    public static Unpacker<Vector> createUnpackerStatic(SendProp prop) {
        return new VectorXYUnpacker(FloatUnpackerFactory.createUnpackerStatic(prop));
    }

    @Override
    public Unpacker<Vector> createUnpacker(SendProp prop) {
        return createUnpackerStatic(prop);
    }
}
