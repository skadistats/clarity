package skadistats.clarity.decoder.s2.prop;

import skadistats.clarity.decoder.BitStream;
import skadistats.clarity.decoder.s2.FieldDecoder;
import skadistats.clarity.model.Vector;
import skadistats.clarity.model.s2.Field;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class QAngleDecoder implements FieldDecoder<Vector> {

    @Override
    public Vector decode(BitStream bs, Field f) {
        float[] v = new float[3];
        if (f.getBitCount() != null && f.getBitCount().intValue() != 0) {
            if (f.getBitCount() == 32) {
                if (true) throw new RuntimeException("QAngle0 " + f.getBitCount());
                v[0] = ByteBuffer.wrap(bs.readBytes(32)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                v[1] = ByteBuffer.wrap(bs.readBytes(32)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                v[2] = ByteBuffer.wrap(bs.readBytes(32)).order(ByteOrder.LITTLE_ENDIAN).getFloat();
                return new Vector(v);
            } else {
                //if (true) throw new RuntimeException("QAngle1 " + f.getBitCount());
                v[0] = bs.readBitAngle(f.getBitCount());
                v[1] = bs.readBitAngle(f.getBitCount());
                v[2] = bs.readBitAngle(f.getBitCount());
                return new Vector(v);
            }
        } else {
            boolean b0 = bs.readUInt(1) == 1;
            boolean b1 = bs.readUInt(1) == 1;
            boolean b2 = bs.readUInt(1) == 1;
            if (b0) v[0] = bs.readBitCoord();
            if (b1) v[1] = bs.readBitCoord();
            if (b2) v[2] = bs.readBitCoord();
            return new Vector(v);
        }
    }
}
