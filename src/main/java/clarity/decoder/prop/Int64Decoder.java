package clarity.decoder.prop;

import clarity.decoder.EntityBitStream;
import clarity.model.Prop;
import clarity.model.PropFlag;

public class Int64Decoder implements PropDecoder<Long> {

    @Override
    public Long decode(EntityBitStream stream, Prop prop) {
        boolean negate = false;
        int remainder = prop.getNumBits() - 32;
        if (!prop.isFlagSet(PropFlag.UNSIGNED)) {
            remainder -= 1;
            negate = stream.readBit();
        }
        long l = stream.readNumericBits(32);
        long r = stream.readNumericBits(remainder);
        long v = (l << 32) | r;
        return negate ? -v : v;
    }

}
