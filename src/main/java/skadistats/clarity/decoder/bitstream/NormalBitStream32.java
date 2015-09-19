package skadistats.clarity.decoder.bitstream;

import com.google.protobuf.ByteString;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.decoder.Util;

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
    public byte[] readBitsAsByteArray(int n) {
        int nBytes = (n + 7) / 8;
        byte[] result = new byte[nBytes];
        if ((pos & 7) == 0) {
            Util.byteCopy(data, pos >> 3, result, 0, nBytes);
            pos += n;
            return result;
        }
        int i = 0;
        while (n > 7) {
            result[i++] = (byte) readUBitInt(8);
            n -= 8;
        }
        if (n != 0) {
            result[i] = (byte) readUBitInt(n);
        }
        return result;
    }



}
