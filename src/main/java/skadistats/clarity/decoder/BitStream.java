package skadistats.clarity.decoder;

import com.google.protobuf.ByteString;
import com.google.protobuf.ZeroCopy;
import org.xerial.snappy.Snappy;

import java.io.IOException;

public class BitStream {

    private final long[] masks = {
        0x0L,             0x1L,              0x3L,              0x7L,
        0xfL,             0x1fL,             0x3fL,             0x7fL,
        0xffL,            0x1ffL,            0x3ffL,            0x7ffL,
        0xfffL,           0x1fffL,           0x3fffL,           0x7fffL,
        0xffffL,          0x1ffffL,          0x3ffffL,          0x7ffffL,
        0xfffffL,         0x1fffffL,         0x3fffffL,         0x7fffffL,
        0xffffffL,        0x1ffffffL,        0x3ffffffL,        0x7ffffffL,
        0xfffffffL,       0x1fffffffL,       0x3fffffffL,       0x7fffffffL,
        0xffffffffL,      0x1ffffffffL,      0x3ffffffffL,      0x7ffffffffL,
        0xfffffffffL,     0x1fffffffffL,     0x3fffffffffL,     0x7fffffffffL,
        0xffffffffffL,    0x1ffffffffffL,    0x3ffffffffffL,    0x7ffffffffffL,
        0xfffffffffffL,   0x1fffffffffffL,   0x3fffffffffffL,   0x7fffffffffffL,
        0xffffffffffffL,  0x1ffffffffffffL,  0x3ffffffffffffL,  0x7ffffffffffffL,
        0xfffffffffffffL, 0x1fffffffffffffL, 0x3fffffffffffffL, 0x7fffffffffffffL
    };

    final long[] data;
    int len;
    int pos;

    public BitStream(ByteString input) {
        len = input.size();
        data = new long[(len + 15) >> 3];
        pos = 0;
        try {
            Snappy.arrayCopy(ZeroCopy.extract(input), 0, len, data, 0);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        len = len * 8; // from now on size in bits
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

    public int readBits(int n) {
        int start = pos >> 6;
        int end = (pos + n - 1) >> 6;
        int s = pos & 63;
        long ret;

        if (start == end) {
            ret = (data[start] >>> s) & masks[n];
        } else { // wrap around
            ret = ((data[start] >>> s) | (data[end] << (64 - s))) & masks[n];
        }
        pos += n;
        return (int) ret;
    }

    public byte[] readBytes(int n) {
        byte[] result = new byte[(n + 7) / 8];
        int i = 0;
        while (n > 7) {
            n -= 8;
            result[i] = (byte) readBits(8);
            i++;
        }
        if (n != 0) {
            result[i] = (byte) readBits(n);
        }
        return result;
    }

    public String readString(int n) {
        StringBuilder buf = new StringBuilder();
        while (n > 0) {
            char c = (char) readBits(8);
            if (c == 0) {
                break;
            }
            buf.append(c);
            n--;
        }
        return buf.toString();
    }

    public int readVarUInt32() {
        int s = 0;
        int v = 0;
        int b;
        while (true) {
            b = readBits(8);
            v |= (b & 0x7F) << s;
            s += 7;
            if ((b & 0x80) == 0 || s == 35) {
                return v;
            }
        }
    }

    public int readVarInt32() {
        int v = readVarUInt32();
        return (v >> 1) ^ -(v & 1);
    }

    public int readUBitVar() {
        // Thanks to Robin Dietrich for providing a clean version of this code :-)

        // The header looks like this: [XY00001111222233333333333333333333] where everything > 0 is optional.
        // The first 2 bits (X and Y) tell us how much (if any) to read other than the 6 initial bits:
        // Y set -> read 4
        // X set -> read 8
        // X + Y set -> read 28

        int v = readBits(6);
        switch (v & 48) {
            case 16:
                v = (v & 15) | (readBits(4) << 4);
                break;
            case 32:
                v = (v & 15) | (readBits(8) << 4);
                break;
            case 48:
                v = (v & 15) | (readBits(28) << 4);
                break;
        }
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

    private int peekBit(int pos) {
        int start = pos >> 6;
        int s = pos & 63;
        long ret = (data[start] >>> s) & masks[1];
        return (int) ret;
    }


}
