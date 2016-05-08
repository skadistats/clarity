package skadistats.clarity.decoder.bitstream;

import com.google.protobuf.ByteString;
import skadistats.clarity.decoder.s2.FieldOpType;

import java.lang.reflect.Constructor;

public abstract class BitStream {

    private static final int COORD_INTEGER_BITS = 14;
    private static final int COORD_FRACTIONAL_BITS = 5;
    private static final float COORD_RESOLUTION = (1.0f / (1 << COORD_FRACTIONAL_BITS));

    private static final int COORD_INTEGER_BITS_MP = 11;
    private static final int COORD_FRACTIONAL_BITS_MP_LOWPRECISION = 3;
    private static final int COORD_DENOMINATOR_LOWPRECISION = (1 << COORD_FRACTIONAL_BITS_MP_LOWPRECISION);
    private static final float COORD_RESOLUTION_LOWPRECISION = (1.0f / COORD_DENOMINATOR_LOWPRECISION);

    private static final int NORMAL_FRACTIONAL_BITS = 11;
    private static final float NORMAL_FRACTIONAL_RESOLUTION = (1.0f / ((1 << NORMAL_FRACTIONAL_BITS) - 1));

    public static final long[] MASKS = {
        0x0L,               0x1L,                0x3L,                0x7L,
        0xfL,               0x1fL,               0x3fL,               0x7fL,
        0xffL,              0x1ffL,              0x3ffL,              0x7ffL,
        0xfffL,             0x1fffL,             0x3fffL,             0x7fffL,
        0xffffL,            0x1ffffL,            0x3ffffL,            0x7ffffL,
        0xfffffL,           0x1fffffL,           0x3fffffL,           0x7fffffL,
        0xffffffL,          0x1ffffffL,          0x3ffffffL,          0x7ffffffL,
        0xfffffffL,         0x1fffffffL,         0x3fffffffL,         0x7fffffffL,
        0xffffffffL,        0x1ffffffffL,        0x3ffffffffL,        0x7ffffffffL,
        0xfffffffffL,       0x1fffffffffL,       0x3fffffffffL,       0x7fffffffffL,
        0xffffffffffL,      0x1ffffffffffL,      0x3ffffffffffL,      0x7ffffffffffL,
        0xfffffffffffL,     0x1fffffffffffL,     0x3fffffffffffL,     0x7fffffffffffL,
        0xffffffffffffL,    0x1ffffffffffffL,    0x3ffffffffffffL,    0x7ffffffffffffL,
        0xfffffffffffffL,   0x1fffffffffffffL,   0x3fffffffffffffL,   0x7fffffffffffffL,
        0xffffffffffffffL,  0x1ffffffffffffffL,  0x3ffffffffffffffL,  0x7ffffffffffffffL,
        0xfffffffffffffffL, 0x1fffffffffffffffL, 0x3fffffffffffffffL, 0x7fffffffffffffffL,
        0xffffffffffffffffL
    };

    protected int len;
    protected int pos;

    private static final Constructor<BitStream> bitStreamConstructor = BitStreamImplementations.determineConstructor();

    public static BitStream createBitStream(ByteString input) {
        try {
            return bitStreamConstructor.newInstance(input);
        } catch (Exception e) {
            throw (RuntimeException) e;
        }
    }

    protected abstract int peekBit(int pos);
    public abstract int readUBitInt(int n);
    public abstract long readUBitLong(int n);
    public abstract void readBitsIntoByteArray(byte[] dest, int n);
    public abstract FieldOpType readFieldOp();

    public int len() {
        return len;
    }

    public int pos() {
        return pos;
    }

    public int remaining() {
        return len - pos;
    }

    public void skip(int n) {
        pos = pos + n;
    }

    public int readBit() {
        return peekBit(pos++);
    }

    public boolean readBitFlag() {
        return peekBit(pos++) != 0;
    }

    public long readSBitLong(int n) {
        long v = readUBitLong(n);
        return (v & (1L << (n - 1))) == 0 ? v : v | (MASKS[64 - n] << n);
    }

    public int readSBitInt(int n) {
        int v = readUBitInt(n);
        return (v & (1 << (n - 1))) == 0 ? v : v | ((int)MASKS[32 - n] << n);
    }

    public String readString(int n) {
        StringBuilder buf = new StringBuilder();
        while (n > 0) {
            char c = (char) readUBitInt(8);
            if (c == 0) {
                break;
            }
            buf.append(c);
            n--;
        }
        return buf.toString();
    }

    public long readVarU(int max) {
        int m = ((max + 6) / 7) * 7;
        int s = 0;
        long v = 0L;
        long b;
        while (true) {
            b = readUBitInt(8);
            v |= (b & 0x7FL) << s;
            s += 7;
            if ((b & 0x80L) == 0L || s == m) {
                return v;
            }
        }
    }

    public long readVarS(int max) {
        long v = readVarU(max);
        return (v >>> 1) ^ -(v & 1L);
    }

    public long readVarULong() {
        return readVarU(64);
    }

    public long readVarSLong() {
        return readVarS(64);
    }

    public int readVarUInt() {
        return (int) readVarU(32);
    }

    public int readVarSInt() {
        return (int) readVarS(32);
    }

    public int readUBitVar() {
        // Thanks to Robin Dietrich for providing a clean version of this code :-)

        // The header looks like this: [XY00001111222233333333333333333333] where everything > 0 is optional.
        // The first 2 bits (X and Y) tell us how much (if any) to read other than the 6 initial bits:
        // Y set -> read 4
        // X set -> read 8
        // X + Y set -> read 28

        int v = readUBitInt(6);
        switch (v & 48) {
            case 16:
                v = (v & 15) | (readUBitInt(4) << 4);
                break;
            case 32:
                v = (v & 15) | (readUBitInt(8) << 4);
                break;
            case 48:
                v = (v & 15) | (readUBitInt(28) << 4);
                break;
        }
        return v;
    }

    public int readUBitVarFieldPath() {
        if (readBitFlag()) return readUBitInt(2);
        if (readBitFlag()) return readUBitInt(4);
        if (readBitFlag()) return readUBitInt(10);
        if (readBitFlag()) return readUBitInt(17);
        return readUBitInt(31);
    }

    public float readBitCoord() {
        boolean i = readBitFlag(); // integer component present?
        boolean f = readBitFlag(); // fractional component present?
        float v = 0.0f;
        if (!(i || f)) return v;
        boolean s = readBitFlag();
        if (i) v = (float)(readUBitInt(COORD_INTEGER_BITS) + 1);
        if (f) v += readUBitInt(COORD_FRACTIONAL_BITS) * COORD_RESOLUTION;
        return s ? -v : v;
    }

    public float readCellCoord(int n, boolean integral, boolean lowPrecision) {
        float v = (float)(readUBitInt(n));
        if (integral) {
            // TODO: something weird is going on here in alice, we might need to adjust the sign?
            return v;
        }
        if (lowPrecision) {
            throw new UnsupportedOperationException("implement me!");
        }
        return v + readUBitInt(COORD_FRACTIONAL_BITS) * COORD_RESOLUTION;
    }

    public float readCoordMp(BitStream stream, boolean integral, boolean lowPrecision) {
        int i = 0;
        int f = 0;
        boolean sign = false;
        float value = 0.0f;

        boolean inBounds = stream.readBitFlag();
        if (integral) {
            if (readBitFlag()) {
                sign = stream.readBitFlag();
                value = stream.readUBitInt(inBounds ? COORD_INTEGER_BITS_MP : COORD_INTEGER_BITS) + 1;
            }
        } else {
            if (readBitFlag()) {
                sign = stream.readBitFlag();
                i = stream.readUBitInt(inBounds ? COORD_INTEGER_BITS_MP : COORD_INTEGER_BITS) + 1;
            } else {
                sign = stream.readBitFlag();
            }
            f = stream.readUBitInt(lowPrecision ? COORD_FRACTIONAL_BITS_MP_LOWPRECISION : COORD_FRACTIONAL_BITS);
            value = i + ((float) f * (lowPrecision ? COORD_RESOLUTION_LOWPRECISION : COORD_RESOLUTION));
        }
        return sign ? -value : value;
    }

    public float readBitAngle(int n) {
        return readUBitInt(n) * 360.0f / (1 << n);
    }

    public float readBitNormal() {
        boolean s = readBitFlag();
        float v = (float) readUBitInt(NORMAL_FRACTIONAL_BITS) * NORMAL_FRACTIONAL_RESOLUTION;
        return s ? -v : v;
    }

    public float[] read3BitNormal() {
        float[] v = new float[3];
        boolean x = readBitFlag();
        boolean y = readBitFlag();
        if (x) v[0] = readBitNormal();
        if (y) v[1] = readBitNormal();
        boolean s = readBitFlag();
        float p = v[0] * v[0] + v[1] * v[1];
        if (p < 1.0f) v[2] = (float) Math.sqrt(1.0f - p);
        if (s) v[2] = -v[2];
        return v;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        buf.append('[');
        buf.append(pos);
        buf.append('/');
        buf.append(len);
        buf.append(']');
        buf.append(' ');
        int prefixLen = buf.length();
        int min = Math.max(0, (pos - 32));
        int max = Math.min(len - 1, pos + 64);
        for (int i = min; i <= max; i++) {
            buf.append(peekBit(i));
        }
        buf.insert(pos - min + prefixLen, '*');
        return buf.toString();
    }

    public String toString(int from, int to) {
        StringBuilder buf = new StringBuilder();
        for (int i = from; i < to; i++) {
            buf.append(peekBit(i));
        }
        return buf.toString();
    }

}
