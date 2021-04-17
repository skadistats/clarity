package skadistats.clarity.io.bitstream;

import skadistats.clarity.io.s2.FieldOpHuffmanTree;
import skadistats.clarity.io.s2.FieldOpType;
import skadistats.clarity.platform.buffer.Buffer;

public class BitStream64 extends BitStream {

    private final Buffer buffer;

    public BitStream64(Buffer buffer) {
        this.buffer = buffer;
    }

    protected int peekBit(int pos) {
        int pb = pos >> 3;
        return (buffer.getByte(pb) >> (pos & 7)) & 1;
    }

    @Override
    public int readUBitInt(int n) {
        assert n <= 32;
        int start = (pos >> 3) & 0xFFFFFFF8;
        int end = ((pos + n - 1) >> 3) & 0xFFFFFFF8;
        int s = pos & 63;
        pos += n;
        return (int)(((buffer.getLong(start) >>> s) | (buffer.getLong(end) << (64 - s))) & MASKS[n]);
    }

    @Override
    public long readUBitLong(int n) {
        assert n <= 64;
        int start = (pos >> 3) & 0xFFFFFFF8;
        int end = ((pos + n - 1) >> 3) & 0xFFFFFFF8;
        int s = pos & 63;
        pos += n;
        return ((buffer.getLong(start) >>> s) | (buffer.getLong(end) << (64 - s))) & MASKS[n];
    }

    @Override
    public void readBitsIntoByteArray(Buffer dest, int n) {
        int pb = pos >> 3;
        int nBytes = (n + 7) >> 3;
        if ((pos & 7) == 0) {
            buffer.copy(pb, dest, nBytes);
            pos += n;
            return;
        }
        int src = pb & 0xFFFFFFF8;
        int dst = 0;
        int s = pos & 63;
        pos += n;
        long v;
        while (n >= 64) {
            v = buffer.getLong(src) >>> s;
            src += 8;
            v |= buffer.getLong(src) << (64 - s);
            dest.putLong(dst, v);
            dst += 8;
            n -= 64;
        }
        if (n > 0) {
            long m = MASKS[n];
            v = buffer.getLong(src) >>> s;
            src += 8;
            v |= buffer.getLong(src) << (64 - s);
            v &= m;
            v |= dest.getLong(dst) & ~m;
            dest.putLong(dst, v);
        }
    }

    @Override
    public FieldOpType readFieldOp() {
        int offs = pos >> 3 & 0xFFFFFFF8;
        long v = buffer.getLong(offs);
        long s = 1L << (pos & 63);
        int i = 0;
        while (true) {
            pos++;
            i = FieldOpHuffmanTree.tree[i][(v & s) != 0 ? 1 : 0];
            if (i < 0) {
                return FieldOpHuffmanTree.ops[-i - 1];
            }
            s = s << 1;
            if (s == 0) {
                offs += 8;
                v = buffer.getLong(offs);
                s = 1;
            }
        }
    }

}
