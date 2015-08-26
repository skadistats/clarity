package skadistats.clarity.decoder.field;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.MH;
import skadistats.clarity.model.Vector;

import java.lang.invoke.MethodHandle;

public class QAngleDecoder extends Decoder {

    public static final MethodHandle decodeDefaultNoBitCount = MH.handle(
        QAngleDecoder.class, "decodeDefaultNoBitCount", Vector.class, BitStream.class
    );

    public static Vector decodeDefaultNoBitCount(BitStream bs) {
        float[] v = new float[3];
        boolean b0 = bs.readBitFlag();
        boolean b1 = bs.readBitFlag();
        boolean b2 = bs.readBitFlag();
        if (b0) v[0] = bs.readBitCoord();
        if (b1) v[1] = bs.readBitCoord();
        if (b2) v[2] = bs.readBitCoord();
        return new Vector(v);
    }


    public static final MethodHandle decodeDefaultBitCount = MH.handle(
        QAngleDecoder.class, "decodeDefaultBitCount", Vector.class, BitStream.class, int.class
    );

    public static Vector decodeDefaultBitCount(BitStream bs, int nBits) {
        float[] v = new float[3];
        v[0] = bs.readBitAngle(nBits);
        v[1] = bs.readBitAngle(nBits);
        v[2] = bs.readBitAngle(nBits);
        return new Vector(v);
    }


    public static final MethodHandle decodeDefaultNoScale = MH.handle(
        QAngleDecoder.class, "decodeDefaultNoScale", Vector.class, BitStream.class
    );

    public static Vector decodeDefaultNoScale(BitStream bs) {
        float[] v = new float[3];
        v[0] = Float.intBitsToFloat(bs.readUBitInt(32));
        v[1] = Float.intBitsToFloat(bs.readUBitInt(32));
        v[2] = Float.intBitsToFloat(bs.readUBitInt(32));
        return new Vector(v);
    }


    public static final MethodHandle decodePitchYawOnly = MH.handle(
        QAngleDecoder.class, "decodePitchYawOnly", Vector.class, BitStream.class, int.class
    );

    public static Vector decodePitchYawOnly(BitStream bs, int nBits) {
        if ((nBits | 0x20) == 0x20) {
            throw new RuntimeException("implement me!");
        }
        float[] v = new float[3];
        v[0] = bs.readBitAngle(nBits);
        v[1] = bs.readBitAngle(nBits);
        v[2] = 0.0f;
        return new Vector(v);
    }

}
