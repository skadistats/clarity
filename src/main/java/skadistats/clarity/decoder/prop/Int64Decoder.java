package skadistats.clarity.decoder.prop;

import skadistats.clarity.decoder.EntityBitStream;
import skadistats.clarity.model.Prop;
import skadistats.clarity.model.PropFlag;

public class Int64Decoder implements PropDecoder<Long> {

    @Override
    public Long decode(EntityBitStream stream, Prop prop) {
        boolean isUnsigned = prop.isFlagSet(PropFlag.UNSIGNED);
        if (prop.isFlagSet(PropFlag.ENCODED_AGAINST_TICKCOUNT)) {
            // this integer is encoded against tick count (?)...
            // in this case, we read a protobuf-style varint
            long v = stream.readVarInt();
            if (isUnsigned) {
                return v; // as is -- why?
            }

            // ostensibly, this is the "decoding" part in signed cases
            //System.out.println("encoded against tickcount is " + prop + ": " + v);
            return (-(v & 1L)) ^ (v >>> 1L);
        }

        boolean negate = false;
        int remainder = prop.getNumBits() - 32;
        if (!prop.isFlagSet(PropFlag.UNSIGNED)) {
            remainder -= 1;
            negate = stream.readBit();
        }
        long l = stream.readNumericBits(32);
        long r = stream.readNumericBits(remainder);
        long v = (r << 32) | l;
        return negate ? -v : v;
    }

}
