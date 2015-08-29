package skadistats.clarity.decoder.unpacker;

import skadistats.clarity.decoder.BitStream;

public class FloatDefaultUnpacker implements Unpacker<Float> {

    private final int nBits;
    private final int flags;
    private final float low;
    private final float high;

    public FloatDefaultUnpacker(int nBits, int flags, float low, float high) {
        this.nBits = nBits;
        this.flags = flags;
        this.low = low;
        this.high = high;
    }

    @Override
    public Float unpack(BitStream bs) {
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

}
