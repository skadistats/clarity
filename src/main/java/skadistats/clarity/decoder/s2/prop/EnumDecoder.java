package skadistats.clarity.decoder.s2.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.s2.FieldDecoder;
import skadistats.clarity.model.s2.Field;

public class EnumDecoder implements FieldDecoder<Long> {

    @Override
    public Long decode(BitStream bs, Field f) {
        return bs.readVarS(64);
    }
}
