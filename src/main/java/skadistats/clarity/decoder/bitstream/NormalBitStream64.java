package skadistats.clarity.decoder.bitstream;

import com.google.protobuf.ByteString;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.decoder.s2.FieldOpHuffmanTree;
import skadistats.clarity.decoder.s2.FieldOpType;

public class NormalBitStream64 extends BitStream {

    private final long[] data;

    protected NormalBitStream64(ByteString input) {
        len = input.size();
        data = new long[(len + 15)  >> 3];
        pos = 0;
        Util.byteCopy(ZeroCopy.extract(input), 0, data, 0, len);
        len = len * 8; // from now on size in bits
    }

    protected int peekBit(int pos) {
        return (int)((data[pos >> 6] >> (pos & 63)) & 1L);
    }

    @Override
    public int readUBitInt(int n) {
        assert n <= 32;
        int start = pos >> 6;
        int end = (pos + n - 1) >> 6;
        int s = pos & 63;
        pos += n;
        if (start == end) {
            return (int)((data[start] >>> s) & MASKS[n]);
        } else { // wrap around
            return (int)(((data[start] >>> s) | (data[end] << (64 - s))) & MASKS[n]);
        }
    }

    @Override
    public long readUBitLong(int n) {
        assert n <= 64;
        int start = pos >> 6;
        int end = (pos + n - 1) >> 6;
        int s = pos & 63;
        pos += n;
        if (start == end) {
            return (data[start] >>> s) & MASKS[n];
        } else { // wrap around
            return ((data[start] >>> s) | (data[end] << (64 - s))) & MASKS[n];
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
        int offs = pos >> 6;
        long b = 1L << (pos & 63);
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
