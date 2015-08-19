package skadistats.clarity.decoder.s2.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.s2.FieldDecoder;
import skadistats.clarity.model.s2.Field;

public class QFloatDecoder implements FieldDecoder<Float> {

    @Override
    public Float decode(BitStream bs, Field f) {
        int flags = f.getEncodeFlags();
        if ((flags & 0x100) != 0) {
            throw new UnsupportedOperationException("read raw 32bit float here");
        } else {
            if ((flags & 0x10) != 0 && bs.readUInt(1) == 1) {
                return f.getLowValue();
            }
            if ((flags & 0x20) != 0 && bs.readUInt(1) == 1) {
                return f.getHighValue();
            }
            int bc = f.getBitCount() != null ? f.getBitCount() : 24; // assume 8, dunno if right, makes m_hOwnerEntity in CWorld baseline look correct ...
            float v = (float) bs.readUInt(bc);
            float low = f.getLowValue() != null ? f.getLowValue() : 0.0f;
            float high = f.getHighValue() != null ? f.getHighValue() : 1.0f;
            return low + (v / BitStream.MASKS[bc]) * (high - low);
        }
    }
}
