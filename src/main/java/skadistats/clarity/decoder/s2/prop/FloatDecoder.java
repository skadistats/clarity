package skadistats.clarity.decoder.s2.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.s2.FieldDecoder;
import skadistats.clarity.model.s2.Field;

public class FloatDecoder implements FieldDecoder<Float> {

    @Override
    public Float decode(BitStream bs, Field f) {
        if ("coord".equals(f.getEncoder())) {
            return bs.readBitCoord();

        } else {
            int v = (f.getBitCount() != 0 ? bs.readUBitInt(f.getBitCount()) : bs.readVarUInt());
            return Float.intBitsToFloat(v);
        }
    }
}
