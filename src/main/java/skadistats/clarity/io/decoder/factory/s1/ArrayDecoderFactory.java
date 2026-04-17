package skadistats.clarity.io.decoder.factory.s1;

import skadistats.clarity.io.Util;
import skadistats.clarity.io.decoder.ArrayDecoder;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.s1.S1DecoderFactory;
import skadistats.clarity.model.s1.SendProp;

public class ArrayDecoderFactory {

    public static Decoder createDecoder(SendProp prop) {
        return new ArrayDecoder(
            S1DecoderFactory.createDecoder(prop.getTemplate()),
            Util.calcBitsNeededFor(prop.getNumElements())
        );
    }

}
