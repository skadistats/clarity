package skadistats.clarity.decoder.field;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.MH;

import java.lang.invoke.MethodHandle;

public class IntDecoder extends Decoder {

    public static final MethodHandle decodeSignedInt = MH.handle(
        IntDecoder.class, "decodeSignedInt", Integer.class, BitStream.class
    );

    public static Integer decodeSignedInt(BitStream bs) {
        return bs.readVarSInt();
    }


    public static final MethodHandle decodeSignedLong = MH.handle(
        IntDecoder.class, "decodeSignedLong", Long.class, BitStream.class
    );

    public static Long decodeSignedLong(BitStream bs) {
        return bs.readVarSLong();
    }


    public static final MethodHandle decodeUnsignedInt = MH.handle(
        IntDecoder.class, "decodeUnsignedInt", Integer.class, BitStream.class
    );

    public static Integer decodeUnsignedInt(BitStream bs) {
        return bs.readVarUInt();
    }


    public static final MethodHandle decodeUnsignedLong = MH.handle(
        IntDecoder.class, "decodeUnsignedLong", Long.class, BitStream.class
    );

    public static Long decodeUnsignedLong(BitStream bs) {
        return bs.readVarULong();
    }


    public static final MethodHandle decodeIntMinusOne = MH.handle(
        IntDecoder.class, "decodeIntMinusOne", Integer.class, BitStream.class
    );

    public static Integer decodeIntMinusOne(BitStream bs) {
        return bs.readVarUInt() - 1;
    }

}
