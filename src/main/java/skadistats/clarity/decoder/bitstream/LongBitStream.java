package skadistats.clarity.decoder.bitstream;

import com.google.protobuf.ByteString;
import com.google.protobuf.ZeroCopy;
import org.xerial.snappy.Snappy;

import java.io.IOException;

public class LongBitStream extends BitStream {

    private final long[] data;

    protected LongBitStream(ByteString input) {
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

    protected int peekBit(int pos) {
        return (int)((data[pos >> 6] >> (pos & 63)) & 1L);
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

}
