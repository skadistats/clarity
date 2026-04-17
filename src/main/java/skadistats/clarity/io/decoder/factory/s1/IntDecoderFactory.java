package skadistats.clarity.io.decoder.factory.s1;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.IntSignedDecoder;
import skadistats.clarity.io.decoder.IntUnsignedDecoder;
import skadistats.clarity.io.decoder.IntVarSignedDecoder;
import skadistats.clarity.io.decoder.IntVarUnsignedDecoder;
import skadistats.clarity.model.s1.PropFlag;
import skadistats.clarity.model.s1.SendProp;

public class IntDecoderFactory {

    public static Decoder createDecoder(SendProp prop) {
        var flags = prop.getFlags();
        if ((flags & PropFlag.ENCODED_AS_VARINT) != 0) {
            if ((flags & PropFlag.UNSIGNED) != 0) {
                return new IntVarUnsignedDecoder();
            } else {
                return new IntVarSignedDecoder();
            }
        } else {
            if ((flags & PropFlag.UNSIGNED) != 0) {
                return new IntUnsignedDecoder(prop.getNumBits());
            } else {
                return new IntSignedDecoder(prop.getNumBits());
            }
        }
    }

}
