package skadistats.clarity.decoder.s2.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.s2.FieldDecoder;
import skadistats.clarity.model.s2.Field;

public class QFloatDecoder implements FieldDecoder<Float> {

    @Override
    public Float decode(BitStream bs, Field f) {
        int flags = f.getEncodeFlags();
        if ((flags & 0x100) != 0) {
            bs.skip(f.getBitCount());
            return 0.0f;
        } else {
            if ((flags & 0x10) != 0 && bs.readUBitInt(1) == 1) {
                return f.getLowValue();
            }
            if ((flags & 0x20) != 0 && bs.readUBitInt(1) == 1) {
                return f.getHighValue();
            }
            if ((flags & 0x40) != 0 && bs.readUBitInt(1) == 1) {
                return 0.0f;
            }
            int bc = f.getBitCount();
            float v = (float) bs.readUBitInt(bc);
            float low = f.getLowValue() != null ? f.getLowValue() : 0.0f;
            float high = f.getHighValue() != null ? f.getHighValue() : 1.0f;
            return low + (v / BitStream.MASKS[bc]) * (high - low);
        }
    }
}
