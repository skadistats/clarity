package skadistats.clarity.decoder.s1.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.model.s1.SendProp;

public class ArrayDecoder implements PropDecoder<Object[]> {

    @Override
    public Object[] decode(BitStream stream, SendProp prop) {
        int count = stream.readBits(Util.calcBitsNeededFor(prop.getNumElements() - 1));
        Object[] result = new Object[count];
        PropDecoder<?> decoder = prop.getTemplate().getType().getDecoder();
        int i = 0;
        while (i < count) {
            result[i] = decoder.decode(stream, prop.getTemplate());
            i++;
        }
        return result;
    }

}
