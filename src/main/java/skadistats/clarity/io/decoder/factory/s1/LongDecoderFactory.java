package skadistats.clarity.io.decoder.factory.s1;

import skadistats.clarity.io.s1.SendProp;
import skadistats.clarity.io.decoder.*;
import skadistats.clarity.model.s1.PropFlag;

public class LongDecoderFactory implements DecoderFactory<Long> {

    public static Decoder<Long> createDecoderStatic(SendProp prop) {
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

    @Override
    public Decoder<Long> createDecoder(SendProp prop) {
        return createDecoderStatic(prop);
    }
}
