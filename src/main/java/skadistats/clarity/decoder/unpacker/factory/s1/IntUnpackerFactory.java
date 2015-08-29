package skadistats.clarity.decoder.unpacker.factory.s1;

import skadistats.clarity.decoder.unpacker.*;
import skadistats.clarity.model.s1.PropFlag;
import skadistats.clarity.model.s1.SendProp;

public class IntUnpackerFactory implements UnpackerFactory<Integer> {

    public static Unpacker<Integer> createUnpackerStatic(SendProp prop) {
        int flags = prop.getFlags();
        if ((flags & PropFlag.ENCODED_AS_VARINT) != 0) {
            if ((flags & PropFlag.UNSIGNED) != 0) {
                return new IntVarUnsignedUnpacker();
            } else {
                return new IntVarSignedUnpacker();
            }
        } else {
            if ((flags & PropFlag.UNSIGNED) != 0) {
                return new IntUnsignedUnpacker(prop.getNumBits());
            } else {
                return new IntSignedUnpacker(prop.getNumBits());
            }
        }
    }

    @Override
    public Unpacker<Integer> createUnpacker(SendProp prop) {
        return createUnpackerStatic(prop);
    }
}
