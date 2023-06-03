package skadistats.clarity.io.s2;

import org.slf4j.Logger;
import skadistats.clarity.io.decoder.BoolDecoder;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.FloatNoScaleDecoder;
import skadistats.clarity.io.decoder.IntMinusOneDecoder;
import skadistats.clarity.io.decoder.IntVarSignedDecoder;
import skadistats.clarity.io.decoder.IntVarUnsignedDecoder;
import skadistats.clarity.io.decoder.LongVarSignedDecoder;
import skadistats.clarity.io.decoder.LongVarUnsignedDecoder;
import skadistats.clarity.io.decoder.StringZeroTerminatedDecoder;
import skadistats.clarity.io.decoder.factory.s2.DecoderFactory;
import skadistats.clarity.io.decoder.factory.s2.FloatDecoderFactory;
import skadistats.clarity.io.decoder.factory.s2.LongUnsignedDecoderFactory;
import skadistats.clarity.io.decoder.factory.s2.QAngleDecoderFactory;
import skadistats.clarity.io.decoder.factory.s2.VectorDecoderFactory;
import skadistats.clarity.logger.PrintfLoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static skadistats.clarity.LogChannel.decoder;

public class S2DecoderFactory {

    private static final Logger log = PrintfLoggerFactory.getLogger(decoder);

    private static final Decoder DEFAULT_DECODER = new IntVarUnsignedDecoder();

    private static final Map<String, DecoderFactory> FACTORIES = new HashMap<>();

    static {
        // Unsigned ints
        FACTORIES.put("uint64", new LongUnsignedDecoderFactory());

        // Floats
        FACTORIES.put("float32", new FloatDecoderFactory());
        FACTORIES.put("CNetworkedQuantizedFloat", new FloatDecoderFactory());
        FACTORIES.put("QAngle", new QAngleDecoderFactory());

        // Specials
        FACTORIES.put("Vector2D", new VectorDecoderFactory(2));
        FACTORIES.put("Vector", new VectorDecoderFactory(3));
        FACTORIES.put("Vector4D", new VectorDecoderFactory(4));
        FACTORIES.put("Quaternion", new VectorDecoderFactory(4));
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

        // Colors
        DECODERS.put("Color", new IntVarUnsignedDecoder());
        DECODERS.put("color32", new IntVarUnsignedDecoder());

        // Specials
        DECODERS.put("HSequence", new IntMinusOneDecoder());
        DECODERS.put("GameTime_t", new FloatNoScaleDecoder());
    }


    public static DecoderHolder createDecoder(String type) {
        return createDecoder(DecoderProperties.DEFAULT, type);
    }

    public static DecoderHolder createDecoder(DecoderProperties decoderProperties, String type) {
        Decoder decoder;
        var decoderFactory = FACTORIES.get(type);
        if (decoderFactory != null) {
            decoder = decoderFactory.createDecoder(decoderProperties);
        } else {
            decoder = DECODERS.get(type);
            if (decoder == null) {
                log.debug("don't know how to create decoder for %s, assuming int.", type);
                decoder = DEFAULT_DECODER;
            }
        }
        return new DecoderHolder(decoderProperties, decoder);
    }

}
