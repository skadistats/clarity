package skadistats.clarity.decoder.s2;

import org.slf4j.Logger;
import skadistats.clarity.decoder.s2.field.FieldProperties;
import skadistats.clarity.decoder.unpacker.BoolUnpacker;
import skadistats.clarity.decoder.unpacker.IntMinusOneUnpacker;
import skadistats.clarity.decoder.unpacker.IntVarSignedUnpacker;
import skadistats.clarity.decoder.unpacker.IntVarUnsignedUnpacker;
import skadistats.clarity.decoder.unpacker.LongVarSignedUnpacker;
import skadistats.clarity.decoder.unpacker.LongVarUnsignedUnpacker;
import skadistats.clarity.decoder.unpacker.StringZeroTerminatedUnpacker;
import skadistats.clarity.decoder.unpacker.Unpacker;
import skadistats.clarity.decoder.unpacker.factory.s2.FloatUnpackerFactory;
import skadistats.clarity.decoder.unpacker.factory.s2.LongUnsignedUnpackerFactory;
import skadistats.clarity.decoder.unpacker.factory.s2.QAngleUnpackerFactory;
import skadistats.clarity.decoder.unpacker.factory.s2.UnpackerFactory;
import skadistats.clarity.decoder.unpacker.factory.s2.VectorUnpackerFactory;
import skadistats.clarity.logger.PrintfLoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static skadistats.clarity.LogChannel.unpacker;

public class S2UnpackerFactory {

    private static final Logger log = PrintfLoggerFactory.getLogger(unpacker);

    private static final Unpacker DEFAULT_UNPACKER = new IntVarUnsignedUnpacker();

    private static final Map<String, UnpackerFactory> FACTORIES = new HashMap<>();

    static {
        // Unsigned ints
        FACTORIES.put("uint64", new LongUnsignedUnpackerFactory());

        // Floats
        FACTORIES.put("float32", new FloatUnpackerFactory());
        FACTORIES.put("CNetworkedQuantizedFloat", new FloatUnpackerFactory());
        FACTORIES.put("QAngle", new QAngleUnpackerFactory());

        // Specials
        FACTORIES.put("Vector", new VectorUnpackerFactory());
    }

    private static final Map<String, Unpacker> UNPACKERS = new HashMap<>();

    static {
        // Booleans
        UNPACKERS.put("bool", new BoolUnpacker());

        // Unsigned ints
        UNPACKERS.put("uint8", new IntVarUnsignedUnpacker());
        UNPACKERS.put("uint16", new IntVarUnsignedUnpacker());
        UNPACKERS.put("uint32", new IntVarUnsignedUnpacker());

        // Signed ints
        UNPACKERS.put("int8", new IntVarSignedUnpacker());
        UNPACKERS.put("int16", new IntVarSignedUnpacker());
        UNPACKERS.put("int32", new IntVarSignedUnpacker());
        UNPACKERS.put("int64", new LongVarSignedUnpacker());

        // Strings
        UNPACKERS.put("CUtlSymbolLarge", new StringZeroTerminatedUnpacker());
        UNPACKERS.put("char", new StringZeroTerminatedUnpacker());
        UNPACKERS.put("CUtlStringToken", new IntVarUnsignedUnpacker());

        // Handles
        UNPACKERS.put("CHandle", new IntVarUnsignedUnpacker());
        UNPACKERS.put("CEntityHandle", new IntVarUnsignedUnpacker());
        UNPACKERS.put("CGameSceneNodeHandle", new IntVarUnsignedUnpacker());
        UNPACKERS.put("CStrongHandle", new LongVarUnsignedUnpacker());

        // Colors
        UNPACKERS.put("Color", new IntVarUnsignedUnpacker());
        UNPACKERS.put("color32", new IntVarUnsignedUnpacker());

        // Specials
        UNPACKERS.put("HSequence", new IntMinusOneUnpacker());
    }


    public static Unpacker createUnpacker(FieldProperties fieldProperties, String type) {
        UnpackerFactory unpackerFactory = FACTORIES.get(type);
        if (unpackerFactory != null) {
            return unpackerFactory.createUnpacker(fieldProperties);
        }
        Unpacker unpacker = UNPACKERS.get(type);
        if (unpacker == null) {
            log.debug("don't know how to create unpacker for %s, assuming int.", type);
            unpacker = DEFAULT_UNPACKER;
        }
        return unpacker;
    }
}
