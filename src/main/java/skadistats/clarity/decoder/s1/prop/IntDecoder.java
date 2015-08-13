package skadistats.clarity.decoder.s1.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.s1.PropFlag;
import skadistats.clarity.model.s1.SendProp;

public class IntDecoder implements PropDecoder<Integer> {

    @Override
    public Integer decode(BitStream stream, SendProp prop) {
        int v = 0;
        int flags = prop.getFlags();
        boolean isUnsigned = (flags & PropFlag.UNSIGNED) != 0;
        int selfUnsigned = isUnsigned ? PropFlag.UNSIGNED : 0;
        if ((flags & PropFlag.ENCODED_AGAINST_TICKCOUNT) != 0) {
            // this integer is encoded against tick count (?)...
            // in this case, we read a protobuf-style varint
            v = stream.readVarUInt32();
            if (isUnsigned) {
                return v; // as is -- why?
            }

            // ostensibly, this is the "decoding" part in signed cases
            return (-(v & PropFlag.UNSIGNED)) ^ (v >>> PropFlag.UNSIGNED);
        }

        v = stream.readBits(prop.getNumBits());
        int s = (0x80000000 >>> (32 - prop.getNumBits())) & (selfUnsigned - PropFlag.UNSIGNED);
        return (v ^ s) - s;
    }

}
