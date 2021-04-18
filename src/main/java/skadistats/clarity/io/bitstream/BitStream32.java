package skadistats.clarity.io.bitstream;

import skadistats.clarity.io.s2.FieldOpHuffmanTree;
import skadistats.clarity.io.s2.FieldOpType;
import skadistats.clarity.platform.buffer.Buffer;

public class BitStream32 extends BitStream {

    private final Buffer.B32 buffer;

    public BitStream32(Buffer.B32 buffer) {
        this.buffer = buffer;
    }

    protected int peekBit(int pos) {
        return buffer.get(pos >> 5) >> (pos & 31) & 1;
    }

    @Override
    public int readUBitInt(int n) {
        assert n <= 32;
        int start = pos >> 5;
        int end = (pos + n - 1) >> 5;
        int s = pos & 31;
        pos += n;
        return (int)(((buffer.get(start) >>> s) | (buffer.get(end) << (32 - s))) & MASKS[n]);
    }

    @Override
    public long readUBitLong(int n) {
        assert n <= 64;
        if (n > 32) {
            long l = readUBitInt(32);
            long h = readUBitInt(n - 32);
            return h << 32 | l;
        } else {
            return readUBitInt(n);
        }
    }

    @Override
    public void readBitsIntoByteArray(byte[] dest, int n) {
        int o = 0;
        while (n > 8) {
            dest[o++] = (byte)readUBitInt(8);
            n -= 8;
        }
        if (n > 0) {
            dest[o] = (byte)readUBitInt(n);
        }
    }

    @Override
    public FieldOpType readFieldOp() {
        int offs = pos >> 5;
        int s = pos & 31;
        int i = 0;
        int v = buffer.get(offs);
        while (true) {
            pos++;
            i = FieldOpHuffmanTree.tree[i][v >>> s & 1];
            if (i < 0) {
                return FieldOpHuffmanTree.ops[-i - 1];
            }
            if (++s == 32) {
                v = buffer.get(++offs);
                s = 0;
            }
        }
    }
}
