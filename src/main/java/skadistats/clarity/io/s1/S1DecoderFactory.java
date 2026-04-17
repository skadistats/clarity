package skadistats.clarity.io.s1;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.StringLenDecoder;
import skadistats.clarity.io.decoder.factory.s1.ArrayDecoderFactory;
import skadistats.clarity.io.decoder.factory.s1.FloatDecoderFactory;
import skadistats.clarity.io.decoder.factory.s1.IntDecoderFactory;
import skadistats.clarity.io.decoder.factory.s1.LongDecoderFactory;
import skadistats.clarity.io.decoder.factory.s1.VectorDecoderFactory;
import skadistats.clarity.model.s1.PropType;
import skadistats.clarity.model.s1.SendProp;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class S1DecoderFactory {

    private static final Map<PropType, Function<SendProp, Decoder>> FACTORIES = new HashMap<>();
    static {
        FACTORIES.put(PropType.INT, IntDecoderFactory::createDecoder);
        FACTORIES.put(PropType.INT64, LongDecoderFactory::createDecoder);
        FACTORIES.put(PropType.FLOAT, FloatDecoderFactory::createDecoder);
        FACTORIES.put(PropType.VECTOR, prop -> VectorDecoderFactory.createDecoder(3, prop));
        FACTORIES.put(PropType.VECTOR_XY, prop -> VectorDecoderFactory.createDecoder(2, prop));
        FACTORIES.put(PropType.ARRAY, ArrayDecoderFactory::createDecoder);
    }

    private static final Map<PropType, Decoder> DECODERS = new HashMap<>();
    static {
        DECODERS.put(PropType.STRING, new StringLenDecoder());
    }

    public static Decoder createDecoder(SendProp prop) {
        var type = prop.getType();
        var factory = FACTORIES.get(type);
        if (factory != null) {
            return factory.apply(prop);
        }
        return DECODERS.get(type);
    }

}
