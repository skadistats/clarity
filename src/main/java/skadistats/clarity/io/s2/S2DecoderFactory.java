package skadistats.clarity.io.s2;

import org.slf4j.Logger;
import skadistats.clarity.io.decoder.*;
import skadistats.clarity.io.decoder.factory.s2.FloatDecoderFactory;
import skadistats.clarity.io.decoder.factory.s2.LongUnsignedDecoderFactory;
import skadistats.clarity.io.decoder.factory.s2.PointerFactory;
import skadistats.clarity.io.decoder.factory.s2.QAngleDecoderFactory;
import skadistats.clarity.io.decoder.factory.s2.VectorDecoderFactory;
import skadistats.clarity.logger.PrintfLoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static skadistats.clarity.LogChannel.decoder;

public class S2DecoderFactory {

    private static final Logger log = PrintfLoggerFactory.getLogger(decoder);

    private static final Decoder DEFAULT_DECODER = new IntVarUnsignedDecoder();

    private static final Map<String, Function<SerializerProperties, Decoder>> FACTORIES = new HashMap<>();

    static {
        // Unsigned ints
        FACTORIES.put("uint64", LongUnsignedDecoderFactory::createDecoder);

        // Floats
        FACTORIES.put("float32", FloatDecoderFactory::createDecoder);
        FACTORIES.put("CNetworkedQuantizedFloat", FloatDecoderFactory::createDecoder);
        FACTORIES.put("QAngle", QAngleDecoderFactory::createDecoder);

        // Specials
        FACTORIES.put("Vector2D", props -> VectorDecoderFactory.createDecoder(2, props));
        FACTORIES.put("Vector", props -> VectorDecoderFactory.createDecoder(3, props));
        FACTORIES.put("Vector4D", props -> VectorDecoderFactory.createDecoder(4, props));
        FACTORIES.put("Quaternion", props -> VectorDecoderFactory.createDecoder(4, props));
        FACTORIES.put("VectorWS", props -> VectorDecoderFactory.createDecoder(3, props));

        // Pointer
        FACTORIES.put("Pointer", PointerFactory::createDecoder);
    }

    private static final Map<String, Decoder> DECODERS = new HashMap<>();

    static {
        // Booleans
        DECODERS.put("bool", new BoolDecoder());

        // Unsigned ints
        DECODERS.put("uint8", new IntVarUnsignedDecoder());
        DECODERS.put("uint16", new IntVarUnsignedDecoder());
        DECODERS.put("uint32", new IntVarUnsignedDecoder());

        // Signed ints
        DECODERS.put("int8", new IntVarSignedDecoder());
        DECODERS.put("int16", new IntVarSignedDecoder());
        DECODERS.put("int32", new IntVarSignedDecoder());
        DECODERS.put("int64", new LongVarSignedDecoder());

        // Strings
        DECODERS.put("CUtlSymbolLarge", new StringZeroTerminatedDecoder());
        DECODERS.put("char", new StringZeroTerminatedDecoder());
        DECODERS.put("CUtlString", new StringZeroTerminatedDecoder());

        DECODERS.put("CUtlStringToken", new IntVarUnsignedDecoder());

        // Handles
        DECODERS.put("CHandle", new IntVarUnsignedDecoder());
        DECODERS.put("CEntityHandle", new IntVarUnsignedDecoder());
        DECODERS.put("CGameSceneNodeHandle", new IntVarUnsignedDecoder());
        DECODERS.put("CBaseVRHandAttachmentHandle", new IntVarUnsignedDecoder());
        DECODERS.put("CStrongHandle", new LongVarUnsignedDecoder());
        DECODERS.put("ResourceId_t", new LongVarUnsignedDecoder());

        // Colors
        DECODERS.put("Color", new IntVarUnsignedDecoder());
        DECODERS.put("color32", new IntVarUnsignedDecoder());

        // Specials
        DECODERS.put("HSequence", new IntMinusOneDecoder());
        DECODERS.put("GameTime_t", new FloatNoScaleDecoder());
        DECODERS.put("HeroFacetKey_t", new LongVarUnsignedDecoder());
        DECODERS.put("BloodType", new IntUnsignedDecoder(8));
        DECODERS.put("HeroID_t", new IntVarSignedDecoder());
    }


    public static Decoder createDecoder(String type) {
        return createDecoder(SerializerProperties.DEFAULT, type);
    }

    public static Decoder createDecoder(SerializerProperties serializerProperties, String type) {
        var factory = FACTORIES.get(type);
        if (factory != null) {
            return factory.apply(serializerProperties);
        }
        var decoder = DECODERS.get(type);
        if (decoder == null) {
            log.debug("don't know how to create decoder for %s, assuming int.", type);
            decoder = DEFAULT_DECODER;
        }
        return decoder;
    }

}
