package skadistats.clarity.decoder.s2.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.s2.FieldDecoder;
import skadistats.clarity.model.s2.Field;

public class NOPDecoder implements FieldDecoder<Void> {

    @Override
    public Void decode(BitStream bs, Field f) {
        return null;
    }
}
