package skadistats.clarity.decoder.field;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.MH;
import skadistats.clarity.model.Vector;

import java.lang.invoke.MethodHandle;

public class VectorDecoder {

    public static final MethodHandle decodeNormal = MH.handle(
        VectorDecoder.class, "decodeNormal", Vector.class, BitStream.class
    );

    public static Vector decodeNormal(BitStream bs) {
        return new Vector(
            bs.read3BitNormal()
        );
    }


    public static final MethodHandle decodeDefault = MH.handle(
        VectorDecoder.class, "decodeDefault", Vector.class, BitStream.class, MethodHandle.class
    );

    public static Vector decodeDefault(BitStream bs, MethodHandle floatDecoder) throws Throwable {
        return new Vector(
            (Float) floatDecoder.invokeExact(bs),
            (Float) floatDecoder.invokeExact(bs),
            (Float) floatDecoder.invokeExact(bs)
        );
    }

}
