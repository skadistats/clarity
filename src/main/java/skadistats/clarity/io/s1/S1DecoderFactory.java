package skadistats.clarity.io.s1;

import skadistats.clarity.io.decoder.StringLenDecoder;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.factory.s1.*;
import skadistats.clarity.model.s1.PropType;

import java.util.HashMap;
import java.util.Map;

public class S1DecoderFactory {

    private static final Map<PropType, DecoderFactory> FACTORIES = new HashMap<>();
    static {
        FACTORIES.put(PropType.INT, new IntDecoderFactory());
        FACTORIES.put(PropType.INT64, new LongDecoderFactory());
        FACTORIES.put(PropType.FLOAT, new FloatDecoderFactory());
        FACTORIES.put(PropType.VECTOR, new VectorDecoderFactory(3));
        FACTORIES.put(PropType.VECTOR_XY, new VectorDecoderFactory(2));
        FACTORIES.put(PropType.ARRAY, new ArrayDecoderFactory());
    }

    private static final Map<PropType, Decoder> DECODERS = new HashMap<>();
    static {
        DECODERS.put(PropType.STRING, new StringLenDecoder());
    }

    public static Decoder createDecoder(SendProp prop) {
        var type = prop.getType();
        var decoderFactory = FACTORIES.get(type);
        if (decoderFactory != null) {
            return decoderFactory.createDecoder(prop);
        }
        return DECODERS.get(type);
    }

}
