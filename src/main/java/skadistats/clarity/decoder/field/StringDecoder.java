package skadistats.clarity.decoder.field;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.MH;

import java.lang.invoke.MethodHandle;

public class StringDecoder extends Decoder {

    public static final MethodHandle decode = MH.handle(
        StringDecoder.class, "decode", String.class, BitStream.class
    );

    public static String decode(BitStream bs) {
        return bs.readString(Integer.MAX_VALUE);
    }

}
