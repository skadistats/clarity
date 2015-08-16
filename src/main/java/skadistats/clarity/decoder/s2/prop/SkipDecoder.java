package skadistats.clarity.decoder.s2.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.s2.FieldDecoder;
import skadistats.clarity.model.s2.Field;

public class SkipDecoder implements FieldDecoder<Void> {

    private final int bits;

    public SkipDecoder(int bits) {
        this.bits = bits;
    }

    @Override
    public Void decode(BitStream bs, Field f) {
        bs.skip(bits);
        return null;
    }
}
