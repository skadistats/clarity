package skadistats.clarity.decoder;

import com.google.protobuf.ByteString;
import com.google.protobuf.ZeroCopy;
import org.xerial.snappy.Snappy;

import java.io.IOException;

public class BitStream {

    private static final int COORD_INTEGER_BITS = 14;
    private static final int COORD_FRACTIONAL_BITS = 5;
    private static final float COORD_FRACTIONAL_RESOLUTION = (1.0f / (1 << COORD_FRACTIONAL_BITS));

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

    final long[] data;
    int len;
    int pos;

    public BitStream(ByteString input) {
        len = input.size();
        data = new long[(len + 15)  >> 3];
        pos = 0;
        try {
            Snappy.arrayCopy(ZeroCopy.extract(input), 0, len, data, 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        len = len * 8; // from now on size in bits
    }

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

    public boolean readBitFlag() {
        boolean v = (data[pos >> 6] & (1 << (pos & 63))) != 0;
        pos++;
        return v;
    }

    public long readUBitLong(int n) {
        int start = pos >> 6;
        int end = (pos + n - 1) >> 6;
        int s = pos & 63;
        long ret;

        if (start == end) {
            ret = (data[start] >>> s) & MASKS[n];
        } else { // wrap around
            ret = ((data[start] >>> s) | (data[end] << (64 - s))) & MASKS[n];
        }
        pos += n;
        return ret;
    }

    public long readSBitLong(int n) {
        long v = readUBitLong(n);
        return (v & (1L << (n - 1))) == 0 ? v : v | (MASKS[64 - n] << n);
    }

    public byte[] readBitsAsByteArray(int n) {
        byte[] result = new byte[(n + 7) / 8];
        int i = 0;
        while (n > 7) {
            n -= 8;
            result[i] = (byte) readUBitInt(8);
            i++;
        }
        if (n != 0) {
            result[i] = (byte) readUBitInt(n);
        }
        return result;
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
            b = readUBitLong(8);
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

    public int readUBitInt(int n) {
        return (int) readUBitLong(n);
    }

    public int readSBitInt(int n) {
        return (int) readSBitLong(n);
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
        if (readUBitLong(1) == 1L) return readUBitInt(2);
        if (readUBitLong(1) == 1L) return readUBitInt(4);
        if (readUBitLong(1) == 1L) return readUBitInt(10);
        if (readUBitLong(1) == 1L) return readUBitInt(17);
        return readUBitInt(31);
    }

    public float readBitCoord() {
        boolean i = readUBitLong(1) == 1L; // integer component present?
        boolean f = readUBitLong(1) == 1L; // fractional component present?
        float v = 0.0f;
        if (!(i || f)) return v;
        boolean s = readUBitLong(1) == 1L;
        if (i) v = (float)(readUBitLong(COORD_INTEGER_BITS) + 1);
        if (f) v += readUBitLong(COORD_FRACTIONAL_BITS) * COORD_FRACTIONAL_RESOLUTION;
        return s ? -v : v;
    }

    public float readBitAngle(int n) {
        return readUBitLong(n) * 360.0f / (1 << n);
    }

    public float readBitNormal() {
        boolean s = readUBitLong(1) == 1L;
        float v = (float) readUBitLong(NORMAL_FRACTIONAL_BITS) * NORMAL_FRACTIONAL_RESOLUTION;
        return s ? -v : v;
    }

    public float[] read3BitNormal() {
        float[] v = new float[3];
        boolean x = readUBitLong(1) == 1L;
        boolean y = readUBitLong(1) == 1L;
        if (x) v[0] = readBitNormal();
        if (y) v[1] = readBitNormal();
        boolean s = readUBitLong(1) == 1L;
        float p = v[0] * v[0] + v[1] * v[1];
        if (p < 1.0f) v[2] = (float) Math.sqrt(1.0f - p);
        if (s) v[2] = -v[2];
        return v;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();

        int min = Math.max(0, (pos - 32));
        int max = Math.min(data.length * 64 - 1, pos + 64);
        for (int i = min; i <= max; i++) {
            buf.append(peekBit(i));
        }
        buf.insert(pos - min, '*');
        return buf.toString();
    }

    public String toString(int from, int to) {
        StringBuilder buf = new StringBuilder();
        for (int i = from; i < to; i++) {
            buf.append(peekBit(i));
        }
        return buf.toString();
    }

    private int peekBit(int pos) {
        int start = pos >> 6;
        int s = pos & 63;
        long ret = (data[start] >>> s) & MASKS[1];
        return (int) ret;
    }

}
