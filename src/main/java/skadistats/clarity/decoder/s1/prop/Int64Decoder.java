package skadistats.clarity.decoder.s1.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.s1.PropFlag;
import skadistats.clarity.model.s1.SendProp;

public class Int64Decoder implements PropDecoder<Long> {

    @Override
    public Long decode(BitStream stream, SendProp prop) {
        int flags = prop.getFlags();
        boolean isUnsigned = (flags & PropFlag.UNSIGNED) != 0;
        if ((flags & PropFlag.ENCODED_AGAINST_TICKCOUNT) != 0) {
            // this integer is encoded against tick count (?)...
            // in this case, we read a protobuf-style varint
            long v = stream.readVarUInt32();
            if (isUnsigned) {
                return v; // as is -- why?
            }

            // ostensibly, this is the "decoding" part in signed cases
            //System.out.println("encoded against tickcount is " + prop + ": " + v);
            return (-(v & 1L)) ^ (v >>> 1L);
        }

        boolean negate = false;
        int remainder = prop.getNumBits() - 32;
        if ((flags & PropFlag.UNSIGNED) == 0) {
            remainder -= 1;
            negate = stream.readBits(1) == 1;
        }
        long l = stream.readBits(32);
        long r = stream.readBits(remainder);
        long v = (r << 32) | l;
        return negate ? -v : v;
    }

}
