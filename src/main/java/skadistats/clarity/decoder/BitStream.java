package skadistats.clarity.decoder;

import skadistats.clarity.model.PVS;

import java.util.ArrayList;
import java.util.List;

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
    int pos;

    public BitStream(byte[] bytes) {

        data = new long[(bytes.length + 7) / 8];
        pos = 0;

        long akku = 0;
        for (int i = 0; i < bytes.length; i++) {
            int shift = 8 * (i & 7);
            long val = ((long) bytes[i]) & 0xFF;
            akku = akku | (val << shift);
            if ((i & 7) == 7) {
                data[i / 8] = akku;
                akku = 0;
            }
        }
        if ((bytes.length & 7) != 0) {
            data[bytes.length / 8] = akku;
        }
    }

    public int readNumericBits(int n) {
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

    public boolean readBit() {
        return readNumericBits(1) == 1;
    }

    public byte[] readBits(int num) {
        byte[] result = new byte[(num + 7) / 8];
        int i = 0;
        while (num > 7) {
            num -= 8;
            result[i] = (byte) readNumericBits(8);
            i++;
        }
        if (num != 0) {
            result[i] = (byte) readNumericBits(num);
        }
        return result;
    }

    public String readString(int num) {
        StringBuffer buf = new StringBuffer();
        while (num > 0) {
            char c = (char) readNumericBits(8);
            if (c == 0) {
                break;
            }
            buf.append(c);
            num--;
        }
        return buf.toString();
    }

    public int readVarInt() {
        int run = 0;
        int value = 0;

        while (true) {
            int bits = readNumericBits(8);
            value = value | ((bits & 0x7f) << run);
            run += 7;
            if ((bits >> 7) == 0 || run == 35) {
                break;
            }
        }
        return value;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();

        int min = Math.max(0, (pos - 32) / 64);
        int max = Math.min(data.length - 1, (pos + 63) / 64);
        for (int i = min; i <= max; i++) {
            buf.append(new StringBuffer(String.format("%64s", Long.toBinaryString(data[i])).replace(' ', '0')).reverse());
        }
        buf.insert(pos - min * 32, '*');
        return buf.toString();
    }

    public int readEntityIndex(int baseIndex) {
        // Thanks to Robin Dietrich for providing a clean version of this code :-)

        // The header looks like this: [XY00001111222233333333333333333333] where everything > 0 is optional.
        // The first 2 bits (X and Y) tell us how much (if any) to read other than the 6 initial bits:
        // Y set -> read 4
        // X set -> read 8
        // X + Y set -> read 28

        int offset = readNumericBits(6);
        switch (offset & 48) {
            case 16:
                offset = (offset & 15) | (readNumericBits(4) << 4);
                break;
            case 32:
                offset = (offset & 15) | (readNumericBits(8) << 4);
                break;
            case 48:
                offset = (offset & 15) | (readNumericBits(28) << 4);
                break;
        }
        return baseIndex + offset + 1;
    }

    public PVS readEntityPVS() {
        return PVS.values()[(readNumericBits(1) << 1) | readNumericBits(1)];
    }

    public List<Integer> readEntityPropList() {
        List<Integer> propList = new ArrayList<Integer>();
        int cursor = -1;

        while (true) {
            if (readBit()) {
                cursor += 1;
            } else {
                int offset = readVarInt();
                if (offset == 0x3fff) {
                    return propList;
                } else {
                    cursor += offset + 1;
                }
            }
            propList.add(cursor);
        }
    }
}
