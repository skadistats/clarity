package skadistats.clarity.decoder.field;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.MH;

import java.lang.invoke.MethodHandle;

public class BoolDecoder extends Decoder {

    public static final MethodHandle decode = MH.handle(
        BoolDecoder.class, "decode", Boolean.class, BitStream.class
    );

    public static Boolean decode(BitStream bs) {
        return bs.readBitFlag();
    }

}
