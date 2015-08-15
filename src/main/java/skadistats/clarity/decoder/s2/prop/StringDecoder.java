package skadistats.clarity.decoder.s2.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.s2.FieldDecoder;
import skadistats.clarity.model.s2.Field;

public class StringDecoder implements FieldDecoder<String> {

    @Override
    public String decode(BitStream bs, Field f) {
        return bs.readString(Integer.MAX_VALUE);
    }
}
