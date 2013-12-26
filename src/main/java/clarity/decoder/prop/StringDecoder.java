package clarity.decoder.prop;

import clarity.decoder.EntityBitStream;
import clarity.model.Prop;

public class StringDecoder implements PropDecoder<String> {

    @Override
    public String decode(EntityBitStream stream, Prop prop) {
        int len = stream.readNumericBits(9);
        return stream.readString(len);
    }

}
