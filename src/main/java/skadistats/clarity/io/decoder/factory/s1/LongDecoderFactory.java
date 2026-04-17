package skadistats.clarity.io.decoder.factory.s1;

import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.io.decoder.LongSignedDecoder;
import skadistats.clarity.io.decoder.LongUnsignedDecoder;
import skadistats.clarity.io.decoder.LongVarSignedDecoder;
import skadistats.clarity.io.decoder.LongVarUnsignedDecoder;
import skadistats.clarity.model.s1.PropFlag;
import skadistats.clarity.model.s1.SendProp;

public class LongDecoderFactory {

    public static Decoder createDecoder(SendProp prop) {
        var flags = prop.getFlags();
        if ((flags & PropFlag.ENCODED_AS_VARINT) != 0) {
            if ((flags & PropFlag.UNSIGNED) != 0) {
                return new LongVarUnsignedDecoder();
            } else {
                return new LongVarSignedDecoder();
            }
        } else {
            if ((flags & PropFlag.UNSIGNED) != 0) {
                return new LongUnsignedDecoder(prop.getNumBits());
            } else {
                return new LongSignedDecoder(prop.getNumBits());
            }
        }
    }

}
