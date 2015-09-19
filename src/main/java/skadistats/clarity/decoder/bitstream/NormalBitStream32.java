package skadistats.clarity.decoder.bitstream;

import com.google.protobuf.ByteString;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.s2.FieldOpHuffmanTree;
import skadistats.clarity.decoder.s2.FieldOpType;

public class NormalBitStream32 extends BitStream {

    private final int[] data;

    protected NormalBitStream32(ByteString input) {
        len = input.size();
        data = new int[(len + 7)  >> 2];
        pos = 0;
        Util.byteCopy(ZeroCopy.extract(input), 0, data, 0, len);
        len = len * 8; // from now on size in bits
    }

    @Override
    protected int peekBit(int pos) {
        return (data[pos >> 5] >> (pos & 31)) & 1;
    }

    @Override
    public int readUBitInt(int n) {
        assert n <= 32;
        int start = pos >> 5;
        int end = (pos + n - 1) >> 5;
        int s = pos & 31;
        pos += n;
        if (start == end) {
            return (data[start] >>> s) & (int)MASKS[n];
        } else { // wrap around
            return ((data[start] >>> s) | (data[end] << (32 - s))) & (int)MASKS[n];
        }
    }

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
        int nBytes = (n + 7) / 8;
        if ((pos & 7) == 0) {
            Util.byteCopy(data, pos >> 3, dest, 0, nBytes);
            pos += n;
            return;
        }
        int i = 0;
        while (n > 7) {
            dest[i++] = (byte) readUBitInt(8);
            n -= 8;
        }
        if (n != 0) {
            dest[i] = (byte) readUBitInt(n);
        }
    }

    @Override
    public FieldOpType readFieldOp() {
        int offs = pos >> 5;
        int b = 1 << (pos & 31);
        int i = 0;
        while (true) {
            pos++;
            i = FieldOpHuffmanTree.tree[i][(data[offs] & b) != 0 ? 1 : 0];
            if (i < 0) {
                return FieldOpHuffmanTree.ops[-i - 1];
            }
            b = b << 1;
            if (b == 0) {
                offs++;
                b = 1;
            }
        }
    }

}
