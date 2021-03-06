package skadistats.clarity.io.decoder.factory.s1;

import skadistats.clarity.io.Util;
import skadistats.clarity.io.s1.S1DecoderFactory;
import skadistats.clarity.io.s1.SendProp;
import skadistats.clarity.io.decoder.ArrayDecoder;
import skadistats.clarity.io.decoder.Decoder;

public class ArrayDecoderFactory<T> implements DecoderFactory<T> {

    public static Decoder<?> createDecoderStatic(SendProp prop) {
        return new ArrayDecoder(
            S1DecoderFactory.createDecoder(prop.getTemplate()),
            Util.calcBitsNeededFor(prop.getNumElements())
        );
    }

    @Override
    public Decoder<T> createDecoder(SendProp prop) {
        return (Decoder<T>) createDecoderStatic(prop);
    }
}
