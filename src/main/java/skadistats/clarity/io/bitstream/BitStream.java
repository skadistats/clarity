package skadistats.clarity.io.bitstream;

import com.google.protobuf.ByteString;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.model.s2.FieldOpHuffmanTree;
import skadistats.clarity.platform.ClarityPlatform;
import skadistats.clarity.platform.buffer.Buffer;

import java.nio.charset.StandardCharsets;

public class BitStream {

    private static final int COORD_INTEGER_BITS = 14;
    private static final int COORD_FRACTIONAL_BITS = 5;
    private static final float COORD_RESOLUTION = (1.0f / (1 << COORD_FRACTIONAL_BITS));

    private static final int COORD_INTEGER_BITS_MP = 11;
    private static final int COORD_FRACTIONAL_BITS_MP_LOWPRECISION = 3;
    private static final int COORD_DENOMINATOR_LOWPRECISION = (1 << COORD_FRACTIONAL_BITS_MP_LOWPRECISION);
    private static final float COORD_RESOLUTION_LOWPRECISION = (1.0f / COORD_DENOMINATOR_LOWPRECISION);

    private static final int NORMAL_FRACTIONAL_BITS = 11;
    private static final float NORMAL_FRACTIONAL_RESOLUTION = (1.0f / ((1 << NORMAL_FRACTIONAL_BITS) - 1));

    private static final int[] UBV_COUNT = {0, 4, 8, 28};
    private static final int[] UBVFP_COUNT = {2, 4, 10, 17, 31};

    public static final int MAX_STRING_LENGTH = 512;
    private static final ThreadLocal<byte[]> STRING_TEMP = ThreadLocal.withInitial(() -> new byte[MAX_STRING_LENGTH]);

    private final Buffer buffer;
    private int len;
    private int pos;

    public BitStream(Buffer buffer) {
        this.buffer = buffer;
    }

    public static BitStream createBitStream(ByteString input) {
        var bs = ClarityPlatform.createBitStream(ZeroCopy.extract(input));
        bs.len = input.size() * 8;
        return bs;
    }

    private int peekBit(int pos) {
        return (int) (buffer.get(pos >> 6) >> (pos & 63) & 1);
    }

    public int readUBitInt(int n) {
        assert n <= 32;
        var start = pos >> 6;
        var end = (pos + n - 1) >> 6;
        var s = pos & 63;
        pos += n;
        return (int)(((buffer.get(start) >>> s) | (buffer.get(end) << (64 - s))) & (-1L >>> (64 - n)));
    }

    public long readUBitLong(int n) {
        assert n <= 64;
        var start = pos >> 6;
        var end = (pos + n - 1) >> 6;
        var s = pos & 63;
        pos += n;
        return ((buffer.get(start) >>> s) | (buffer.get(end) << (64 - s))) & (-1L >>> (64 - n));
    }

    public void readBitsIntoByteArray(byte[] dest, int n) {
        var o = 0;
        var idx = pos >> 6;
        var s = pos & 63;
        var bulk = n >>> 6;
        if (bulk > 0) {
            if (s == 0) {
                buffer.copyBytesInto(idx << 3, dest, o, bulk << 3);
                o += bulk << 3;
            } else {
                var prev = buffer.get(idx);
                for (var k = 0; k < bulk; k++) {
                    var next = buffer.get(idx + k + 1);
                    buffer.put(dest, o, (prev >>> s) | (next << -s));
                    o += 8;
                    prev = next;
                }
            }
            pos += bulk << 6;
            n   -= bulk << 6;
        }
        while (n > 8) {
            dest[o++] = (byte)readUBitInt(8);
            n -= 8;
        }
        if (n > 0) {
            dest[o] = (byte)readUBitInt(n);
        }
    }

    public int readFieldOpId() {
        var offs = pos >> 6;
        var s = pos & 63;
        var v = buffer.get(offs);

        // Fast path: 8-bit lookup resolves ~99.7% of ops in one step
        var peek = (v >>> s) | (s > 56 ? buffer.get(offs + 1) << (64 - s) : 0);
        var entry = FieldOpHuffmanTree.lookup[(int)(peek & 0xFFL)];
        var bits = entry & 0xFF;
        if (bits != 0) {
            pos += bits;
            return (entry >>> 8) & 0xFF;
        }

        // Slow path: skip lookup bits, continue tree walk from saved node
        var i = (entry >>> 8) & 0xFF;
        pos += FieldOpHuffmanTree.LOOKUP_BITS;
        offs = pos >> 6;
        s = pos & 63;
        v = buffer.get(offs);
        while (true) {
            pos++;
            i = FieldOpHuffmanTree.tree[(i << 1) | (int)((v >>> s) & 1L)];
            if (i < 0) {
                return -i - 1;
            }
            if (++s == 64) {
                v = buffer.get(++offs);
                s = 0;
            }
        }
    }

    public int len() {
        return len;
    }

    public int pos() {
        return pos;
    }

    public void pos(int pos) {
        if (pos >= len) {
            throw new UnsupportedOperationException("pos >= len");
        }
        this.pos = pos;
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
        var v = readUBitLong(n);
        return (v & (1L << (n - 1))) == 0 ? v : v | (((1L << (64 - n)) - 1) << n);
    }

    public int readSBitInt(int n) {
        var v = readUBitInt(n);
        return (v & (1 << (n - 1))) == 0 ? v : v | (((1 << (32 - n)) - 1) << n);
    }

    public String readString(int n) {
        var buf = STRING_TEMP.get();
        var o = 0;
        var limit = Math.min(n, MAX_STRING_LENGTH);
        while (o < limit) {
            var c = (byte) readUBitInt(8);
            if (c == 0) {
                break;
            }
            buf[o++] = c;
        }
        if (o == 0) {
            return "";
        }
        return new String(buf, 0, o, StandardCharsets.UTF_8).intern();
    }

    /**
     * Reads up to {@code n} UTF-8 bytes directly into {@code data} starting at
     * {@code offset}. Stops early on a zero byte (which is consumed from the
     * bitstream but NOT written to {@code data}). Returns the number of bytes
     * written — always {@code <= n}.
     * <p>
     * Used by inline-string decoding to skip the {@code stringTemp} scratch and
     * the {@code String} allocation in {@link #readString}.
     */
    public int readStringInto(byte[] data, int offset, int n) {
        var o = 0;
        while (o < n) {
            var c = (byte) readUBitInt(8);
            if (c == 0) {
                break;
            }
            data[offset + o] = c;
            o++;
        }
        return o;
    }

    public long readVarU(int max) {
        var m = ((max + 6) / 7) * 7;
        var s = 0;
        var v = 0L;
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
        var v = readVarU(max);
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

        var v = readUBitInt(6);
        var a = v >> 4;
        if (a == 0) {
            return v;
        } else {
            return (v & 15) | (readUBitInt(UBV_COUNT[a]) << 4);
        }
    }

    public int readUBitVarFieldPath() {
        var i = -1;
        while (++i < 4) {
            if (readBitFlag()) break;
        }
        return readUBitInt(UBVFP_COUNT[i]);
    }

    public float readBitCoord() {
        var i = readBitFlag(); // integer component present?
        var f = readBitFlag(); // fractional component present?
        var v = 0.0f;
        if (!(i || f)) return v;
        var s = readBitFlag();
        if (i) v = (float)(readUBitInt(COORD_INTEGER_BITS) + 1);
        if (f) v += readUBitInt(COORD_FRACTIONAL_BITS) * COORD_RESOLUTION;
        return s ? -v : v;
    }

    public float readCellCoord(int n, boolean integral, boolean lowPrecision) {
        var v = (float)(readUBitInt(n));
        if (integral) {
            return v;
        }
        if (lowPrecision) {
            return v + readUBitInt(COORD_FRACTIONAL_BITS_MP_LOWPRECISION) * COORD_RESOLUTION_LOWPRECISION;
        }
        return v + readUBitInt(COORD_FRACTIONAL_BITS) * COORD_RESOLUTION;
    }

    public float readCoordMp(BitStream stream, boolean integral, boolean lowPrecision) {
        var i = 0;
        var f = 0;
        var sign = false;
        var value = 0.0f;

        var inBounds = stream.readBitFlag();
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
        var s = readBitFlag();
        var v = (float) readUBitInt(NORMAL_FRACTIONAL_BITS) * NORMAL_FRACTIONAL_RESOLUTION;
        return s ? -v : v;
    }

    public float[] read3BitNormal() {
        var v = new float[3];
        var x = readBitFlag();
        var y = readBitFlag();
        if (x) v[0] = readBitNormal();
        if (y) v[1] = readBitNormal();
        var s = readBitFlag();
        var p = v[0] * v[0] + v[1] * v[1];
        if (p < 1.0f) v[2] = (float) Math.sqrt(1.0f - p);
        if (s) v[2] = -v[2];
        return v;
    }

    public String toString() {
        var buf = new StringBuilder();
        buf.append('[');
        buf.append(pos);
        buf.append('/');
        buf.append(len);
        buf.append(']');
        buf.append(' ');
        var prefixLen = buf.length();
        var min = Math.max(0, (pos - 32));
        var max = Math.min(len - 1, pos + 64);
        for (var i = min; i <= max; i++) {
            buf.append(peekBit(i));
        }
        buf.insert(pos - min + prefixLen, '*');
        return buf.toString();
    }

    public String toString(int from, int to) {
        var buf = new StringBuilder();
        for (var i = from; i < to; i++) {
            buf.append(peekBit(i));
        }
        return buf.toString();
    }

}
