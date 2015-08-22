package skadistats.clarity.decoder.s2.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.s2.FieldDecoder;
import skadistats.clarity.model.Vector;
import skadistats.clarity.model.s2.Field;

public class QAngleDecoder implements FieldDecoder<Vector> {

    @Override
    public Vector decode(BitStream bs, Field f) {
        if ("qangle_pitch_yaw".equals(f.getEncoder())) {
            return pitchYawOnly(bs, f);
        } else {
            return normal(bs, f);
        }
    }

    public Vector pitchYawOnly(BitStream bs, Field f) {
        if (f.getBitCount() == null) {
            throw new RuntimeException("bitcount expected!");
        }
        if ((f.getBitCount() | 0x20) == 0x20) {
            throw new RuntimeException("implement me!");
        }
        float[] v = new float[3];
        v[0] = bs.readBitAngle(f.getBitCount());
        v[1] = bs.readBitAngle(f.getBitCount());
        v[2] = 0.0f;
        return new Vector(v);
    }


    public Vector normal(BitStream bs, Field f) {
        float[] v = new float[3];
        if (f.getBitCount() != null && f.getBitCount().intValue() != 0) {
            if (f.getBitCount() == 32) {
                if (true) throw new RuntimeException("QAngle0 " + f.getBitCount());
                v[0] = Float.intBitsToFloat(bs.readUBitInt(32));
                v[1] = Float.intBitsToFloat(bs.readUBitInt(32));
                v[2] = Float.intBitsToFloat(bs.readUBitInt(32));
                return new Vector(v);
            } else {
                //if (true) throw new RuntimeException("QAngle1 " + f.getBitCount());
                v[0] = bs.readBitAngle(f.getBitCount());
                v[1] = bs.readBitAngle(f.getBitCount());
                v[2] = bs.readBitAngle(f.getBitCount());
                return new Vector(v);
            }
        } else {
            boolean b0 = bs.readUBitInt(1) == 1;
            boolean b1 = bs.readUBitInt(1) == 1;
            boolean b2 = bs.readUBitInt(1) == 1;
            if (b0) v[0] = bs.readBitCoord();
            if (b1) v[1] = bs.readBitCoord();
            if (b2) v[2] = bs.readBitCoord();
            return new Vector(v);
        }
    }
}
