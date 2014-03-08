package skadistats.clarity.decoder.prop;

import skadistats.clarity.decoder.EntityBitStream;
import skadistats.clarity.model.Prop;

public class StringDecoder implements PropDecoder<String> {

    @Override
    public String decode(EntityBitStream stream, Prop prop) {
        int len = stream.readNumericBits(9);
        return stream.readString(len);
    }

}
