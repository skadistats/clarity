package skadistats.clarity.io.bitstream;

import skadistats.clarity.io.s2.FieldOpHuffmanTree;
import skadistats.clarity.io.s2.FieldOpType;
import skadistats.clarity.platform.buffer.Buffer;

public class BitStream64 extends BitStream {

    private final Buffer.B64 buffer;

    public BitStream64(Buffer.B64 buffer) {
        this.buffer = buffer;
    }

    protected int peekBit(int pos) {
        return (int) (buffer.get(pos >> 6) >> (pos & 63) & 1);
    }

    @Override
    public int readUBitInt(int n) {
        assert n <= 32;
        var start = pos >> 6;
        var end = (pos + n - 1) >> 6;
        var s = pos & 63;
        pos += n;
        return (int)(((buffer.get(start) >>> s) | (buffer.get(end) << (64 - s))) & MASKS[n]);
    }

    @Override
    public long readUBitLong(int n) {
        assert n <= 64;
        var start = pos >> 6;
        var end = (pos + n - 1) >> 6;
        var s = pos & 63;
        pos += n;
        return ((buffer.get(start) >>> s) | (buffer.get(end) << (64 - s))) & MASKS[n];
    }

    @Override
    public void readBitsIntoByteArray(byte[] dest, int n) {
        var o = 0;
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
        var offs = pos >> 6;
        var s = pos & 63;
        var i = 0;
        var v = buffer.get(offs);
        while (true) {
            pos++;
            i = FieldOpHuffmanTree.tree[i][(int)((v >>> s) & 1L)];
            if (i < 0) {
                return FieldOpHuffmanTree.ops[-i - 1];
            }
            if (++s == 64) {
                v = buffer.get(++offs);
                s = 0;
            }
        }
    }

}
