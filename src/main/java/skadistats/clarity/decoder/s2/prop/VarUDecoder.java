package skadistats.clarity.decoder.s2.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.s2.FieldDecoder;
import skadistats.clarity.model.s2.Field;

public class VarUDecoder implements FieldDecoder<Long> {

    private final int bits;

    public VarUDecoder(int bits) {
        this.bits = bits;
    }

    @Override
    public Long decode(BitStream bs, Field f) {
        return bs.readVarU(bits);
    }
}
