package skadistats.clarity.util;

import com.google.protobuf.ByteString;
import skadistats.clarity.io.bitstream.BitStream;

import java.io.IOException;

public class LZSS {

    private static final int MAGIC = 'L' | 'Z' << 8 | 'S' << 16 | 'S' << 24;

    public static byte[] unpack(ByteString raw) throws IOException {
        return unpack(BitStream.createBitStream(raw));
    }

    public static byte[] unpack(BitStream bs) throws IOException {
        if (bs.readUBitInt(32) != MAGIC) {
            throw new IOException("wrong LZSS magic");
        }
        var dst = new byte[bs.readUBitInt(32)];
        var di = 0;
        var cmd = 0;
        var bit = 0x100;
        while (true) {
            if (bit == 0x100) {
                cmd = bs.readUBitInt(8);
                bit = 1;
            }
            if ((cmd & bit) == 0) {
                dst[di++] = (byte) bs.readUBitInt(8);
            } else {
                var a = bs.readUBitInt(8);
                var b = bs.readUBitInt(8);
                var offset = 1 + ((a << 4) | (b >> 4));
                var count = 1 + (b & 0x0F);
                if (count == 1) {
                    break;
                }
                var end = di + count;
                for (var i = di; i < end; i++) {
                    dst[i] = dst[i - offset];
                }
                di += count;
            }
            bit = bit << 1;
        }
        return dst;
    }

}
