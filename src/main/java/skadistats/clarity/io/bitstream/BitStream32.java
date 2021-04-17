package skadistats.clarity.io.bitstream;

import skadistats.clarity.io.s2.FieldOpHuffmanTree;
import skadistats.clarity.io.s2.FieldOpType;
import skadistats.clarity.platform.buffer.Buffer;

public class BitStream32 extends BitStream {

    private final Buffer buffer;

    public BitStream32(Buffer buffer) {
        this.buffer = buffer;
    }

    protected int peekBit(int pos) {
        int pb = pos >> 3;
        return buffer.getByte(pb) >> (pos & 7) & 1;
    }

    @Override
    public int readUBitInt(int n) {
        assert n <= 32;
        int start = (pos >> 3) & 0xFFFFFFFC;
        int end = ((pos + n - 1) >> 3) & 0xFFFFFFFC;
        int s = pos & 31;
        pos += n;
        return ((buffer.getInt(start) >>> s) | (buffer.getInt(end) << (32 - s))) & (int)MASKS[n];
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
        int src = pb & 0xFFFFFFFC;
        int dst = 0;
        int s = pos & 31;
        pos += n;
        int v;
        while (n >= 32) {
            v = buffer.getInt(src) >>> s;
            src += 4;
            v |= buffer.getInt(src) << (32 - s);
            dest.putInt(dst, v);
            dst += 4;
            n -= 32;
        }
        if (n > 0) {
            int m = (int)MASKS[n];
            v = buffer.getInt(src) >>> s;
            src += 4;
            v |= buffer.getInt(src) << (32 - s);
            v &= m;
            v |= dest.getInt(dst) & ~m;
            dest.putInt(dst, v);
        }
    }

    @Override
    public FieldOpType readFieldOp() {
        int offs = pos >> 3 & 0xFFFFFFFC;
        int v = buffer.getInt(offs);
        int s = 1 << (pos & 31);
        int i = 0;
        while (true) {
            pos++;
            i = FieldOpHuffmanTree.tree[i][(v & s) != 0 ? 1 : 0];
            if (i < 0) {
                return FieldOpHuffmanTree.ops[-i - 1];
            }
            s = s << 1;
            if (s == 0) {
                offs += 4;
                v = buffer.getInt(offs);
                s = 1;
            }
        }
    }
}
