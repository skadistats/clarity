package skadistats.clarity.decoder.s2.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.s2.FieldDecoder;
import skadistats.clarity.model.s2.Field;

public class BinBlockDecoder implements FieldDecoder<Void> {

    @Override
    public Void decode(BitStream bs, Field f) {
        int n =  bs.readVarUInt();
        bs.skip(n * 8);
        return null;
    }
}
