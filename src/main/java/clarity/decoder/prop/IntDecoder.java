package clarity.decoder.prop;

import clarity.decoder.EntityBitStream;
import clarity.model.Prop;
import clarity.model.PropFlag;

public class IntDecoder implements PropDecoder<Integer> {

	@Override
	public Integer decode(EntityBitStream stream, Prop prop) {
		int v = 0;
		boolean isUnsigned = prop.isFlagSet(PropFlag.UNSIGNED);
		int uFlag = PropFlag.UNSIGNED.getFlag();
		int selfUnsigned = isUnsigned ? uFlag : 0;
        if (prop.isFlagSet(PropFlag.ENCODED_AGAINST_TICKCOUNT)) {
            // this integer is encoded against tick count (?)...
            // in this case, we read a protobuf-style varint
            v = stream.readVarInt();
            if (isUnsigned) {
                return v; // as is -- why?
            }

            // ostensibly, this is the "decoding" part in signed cases
            return (-(v & uFlag)) ^ (v >>> uFlag);
        }
        
        v = stream.readNumericBits(prop.getNumBits());
        int s = (0x80000000 >>> (32 - prop.getNumBits())) & (selfUnsigned - uFlag);
        return (v ^ s) - s;
	}

}
