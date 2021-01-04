package skadistats.clarity.decoder.unpacker.factory.s1;

import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.s1.S1DecoderFactory;
import skadistats.clarity.decoder.s1.SendProp;
import skadistats.clarity.decoder.unpacker.ArrayDecoder;
import skadistats.clarity.decoder.unpacker.Decoder;

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
