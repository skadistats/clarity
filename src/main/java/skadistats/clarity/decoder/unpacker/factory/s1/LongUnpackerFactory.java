package skadistats.clarity.decoder.unpacker.factory.s1;

import skadistats.clarity.decoder.s1.SendProp;
import skadistats.clarity.decoder.unpacker.*;
import skadistats.clarity.model.s1.PropFlag;

public class LongUnpackerFactory implements UnpackerFactory<Long> {

    public static Unpacker<Long> createUnpackerStatic(SendProp prop) {
        int flags = prop.getFlags();
        if ((flags & PropFlag.ENCODED_AS_VARINT) != 0) {
            if ((flags & PropFlag.UNSIGNED) != 0) {
                return new LongVarUnsignedUnpacker();
            } else {
                return new LongVarSignedUnpacker();
            }
        } else {
            if ((flags & PropFlag.UNSIGNED) != 0) {
                return new LongUnsignedUnpacker(prop.getNumBits());
            } else {
                return new LongSignedUnpacker(prop.getNumBits());
            }
        }
    }

    @Override
    public Unpacker<Long> createUnpacker(SendProp prop) {
        return createUnpackerStatic(prop);
    }
}
