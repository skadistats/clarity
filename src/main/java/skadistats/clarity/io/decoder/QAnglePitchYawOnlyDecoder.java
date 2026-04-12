package skadistats.clarity.io.decoder;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.model.Vector;

@RegisterDecoder
public final class QAnglePitchYawOnlyDecoder extends Decoder {

    private final int nBits;

    public QAnglePitchYawOnlyDecoder(int nBits) {
        this.nBits = nBits;
    }

    public static Vector decode(BitStream bs, QAnglePitchYawOnlyDecoder d) {
        var v = new float[3];
        if ((d.nBits | 0x20) == 0x20) {
            v[0] = Float.intBitsToFloat(bs.readUBitInt(32));
            v[1] = Float.intBitsToFloat(bs.readUBitInt(32));
        } else {
            v[0] = bs.readBitAngle(d.nBits);
            v[1] = bs.readBitAngle(d.nBits);
        }
        return new Vector(v);
    }

}
