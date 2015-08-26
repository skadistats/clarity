package skadistats.clarity.decoder.field;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.MH;

import java.lang.invoke.MethodHandle;

public class FloatDecoder extends Decoder {

    public static final MethodHandle decodeNoScale = MH.handle(
        FloatDecoder.class, "decodeNoScale", Float.class, BitStream.class
    );

    public static Float decodeNoScale(BitStream bs) {
        return Float.intBitsToFloat(bs.readUBitInt(32));
    }


    public static final MethodHandle decodeDefault = MH.handle(
        FloatDecoder.class, "decodeDefault", Float.class, BitStream.class, int.class, float.class, float.class, int.class
    );

    public static Float decodeDefault(BitStream bs, int nBits, float low, float high, int flags) {
//        if ((flags & 0x1) != 0 && bs.readBitFlag()) {
//            return low;
//        }
//        if ((flags & 0x2) != 0 && bs.readBitFlag()) {
//            return high;
//        }
        if ((flags & 0x4) != 0 && bs.readBitFlag()) {
            return 0.0f;
        }
        return low + ((float) bs.readUBitInt(nBits) / BitStream.MASKS[nBits]) * (high - low);
    }


    public static final MethodHandle decodeSimulationTime = MH.handle(
        FloatDecoder.class, "decodeSimulationTime", Float.class, BitStream.class
    );

    public static Float decodeSimulationTime(BitStream bs) {
        return (float) bs.readVarULong() * (1.0f / 30.0f);
    }


    public static final MethodHandle decodeCoord = MH.handle(
        FloatDecoder.class, "decodeCoord", Float.class, BitStream.class
    );

    public static Float decodeCoord(BitStream bs) {
        return bs.readBitCoord();
    }


}
