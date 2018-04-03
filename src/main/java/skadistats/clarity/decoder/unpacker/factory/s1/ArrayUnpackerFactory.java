package skadistats.clarity.decoder.unpacker.factory.s1;

import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.s1.S1UnpackerFactory;
import skadistats.clarity.decoder.s1.SendProp;
import skadistats.clarity.decoder.unpacker.ArrayUnpacker;
import skadistats.clarity.decoder.unpacker.Unpacker;

public class ArrayUnpackerFactory<T> implements UnpackerFactory<T> {

    public static Unpacker<?> createUnpackerStatic(SendProp prop) {
        return new ArrayUnpacker(
            S1UnpackerFactory.createUnpacker(prop.getTemplate()),
            Util.calcBitsNeededFor(prop.getNumElements())
        );
    }

    @Override
    public Unpacker<T> createUnpacker(SendProp prop) {
        return (Unpacker<T>) createUnpackerStatic(prop);
    }
}
