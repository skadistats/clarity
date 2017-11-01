package skadistats.clarity.decoder.s1;

import skadistats.clarity.decoder.unpacker.StringLenUnpacker;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.decoder.unpacker.factory.s1.*;
import skadistats.clarity.model.s1.PropType;

import java.util.HashMap;
import java.util.Map;

public class S1UnpackerFactory {

    private static final Map<PropType, UnpackerFactory> FACTORIES = new HashMap<>();
    static {
        FACTORIES.put(PropType.INT, new IntUnpackerFactory());
        FACTORIES.put(PropType.INT64, new LongUnpackerFactory());
        FACTORIES.put(PropType.FLOAT, new FloatUnpackerFactory());
        FACTORIES.put(PropType.VECTOR, new VectorUnpackerFactory(3));
        FACTORIES.put(PropType.VECTOR_XY, new VectorUnpackerFactory(2));
        FACTORIES.put(PropType.ARRAY, new ArrayUnpackerFactory());
    }

    private static final Map<PropType, Unpacker> UNPACKERS = new HashMap<>();
    static {
        UNPACKERS.put(PropType.STRING, new StringLenUnpacker());
    }

    public static Unpacker createUnpacker(SendProp prop) {
        PropType type = prop.getType();
        UnpackerFactory unpackerFactory = FACTORIES.get(type);
        if (unpackerFactory != null) {
            return unpackerFactory.createUnpacker(prop);
        }
        return UNPACKERS.get(type);
    }

}
