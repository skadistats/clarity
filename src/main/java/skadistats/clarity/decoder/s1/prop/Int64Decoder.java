package skadistats.clarity.decoder.s1.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.s1.PropFlag;
import skadistats.clarity.model.s1.SendProp;

public class Int64Decoder implements PropDecoder<Long> {

    @Override
    public Long decode(BitStream stream, SendProp prop) {
        int flags = prop.getFlags();
        if ((flags & PropFlag.ENCODED_AS_VARINT) != 0) {
            if ((flags & PropFlag.UNSIGNED) != 0) {
                return stream.readVarUInt64();
            } else {
                return stream.readVarSInt64();
            }
        } else {
            if ((flags & PropFlag.UNSIGNED) != 0) {
                return stream.readUInt(prop.getNumBits());
            } else {
                return stream.readSInt(prop.getNumBits());
            }
        }
    }

}
