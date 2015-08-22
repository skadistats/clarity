package skadistats.clarity.decoder.s1.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.model.s1.SendProp;

public class StringDecoder implements PropDecoder<String> {

    @Override
    public String decode(BitStream stream, SendProp prop) {
        int len = stream.readUBitInt(9);
        return stream.readString(len);
    }

}
