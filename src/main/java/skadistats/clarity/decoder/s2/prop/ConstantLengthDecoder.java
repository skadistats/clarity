package skadistats.clarity.decoder.s2.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.s2.FieldDecoder;
import skadistats.clarity.model.s2.Field;

public class ConstantLengthDecoder implements FieldDecoder<Long> {

    private final int bits;

    public ConstantLengthDecoder(int bits) {
        this.bits = bits;
    }

    @Override
    public Long decode(BitStream bs, Field f) {
        return bs.readUBitLong(bits);
    }
}
