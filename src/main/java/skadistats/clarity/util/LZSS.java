package skadistats.clarity.util;

import org.xerial.snappy.Snappy;

import java.io.IOException;

public class LZSS {

    public static void unpack(byte[] src, byte[] dst) throws IOException {
        int si = 0;
        int di = 0;
        byte cmd = 0;
        int bit = 0x100;

        while (true) {
            if (bit == 0x100) {
                cmd = src[si++];
                bit = 1;
            }
            if ((cmd & bit) != 0) {
                int a = src[si++];
                int b = src[si++];
                int position = (a << 4) | (b >> 4);
                int count = (b & 0x0F) + 1;
                if (count == 1) {
                    break;
                }
                Snappy.arrayCopy(dst, di - position - 1, count, dst, di);
                di += count;
            } else {
                dst[di++] = src[si++];
            }
            bit = bit << 1;
        }

    }

}
