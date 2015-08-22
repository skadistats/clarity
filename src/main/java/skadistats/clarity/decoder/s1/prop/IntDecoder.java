package skadistats.clarity.decoder.s1.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.s1.PropFlag;
import skadistats.clarity.model.s1.SendProp;

public class IntDecoder implements PropDecoder<Integer> {

    @Override
    public Integer decode(BitStream stream, SendProp prop) {
        int flags = prop.getFlags();
        if ((flags & PropFlag.ENCODED_AS_VARINT) != 0) {
            if ((flags & PropFlag.UNSIGNED) != 0) {
                return stream.readVarUInt();
            } else {
                return stream.readVarSInt();
            }
        } else {
            if ((flags & PropFlag.UNSIGNED) != 0) {
                return stream.readUBitInt(prop.getNumBits());
            } else {
                return stream.readSBitInt(prop.getNumBits());
            }
        }
    }

}
