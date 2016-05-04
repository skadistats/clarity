package skadistats.clarity.util;

import com.google.protobuf.ByteString;
import org.xerial.snappy.Snappy;
import skadistats.clarity.decoder.bitstream.BitStream;

import java.io.IOException;

public class LZSS {

    private static int MAGIC = 'L' | 'Z' << 8 | 'S' << 16 | 'S' << 24;

    public static byte[] unpack(ByteString raw) throws IOException {
        return unpack(BitStream.createBitStream(raw));
    }

    public static byte[] unpack(BitStream bs) throws IOException {
        if (bs.readUBitInt(32) != MAGIC) {
            throw new IOException("wrong LZSS magic");
        }
        byte[] dst = new byte[bs.readUBitInt(32)];
        int di = 0;
        int cmd = 0;
        int bit = 0x100;
        while (true) {
            if (bit == 0x100) {
                cmd = bs.readUBitInt(8);
                bit = 1;
            }
            if ((cmd & bit) == 0) {
                dst[di++] = (byte) bs.readUBitInt(8);
            } else {
                int a = bs.readUBitInt(8);
                int b = bs.readUBitInt(8);
                int position = (a << 4) | (b >> 4);
                int count = (b & 0x0F) + 1;
                if (count == 1) {
                    break;
                }
                Snappy.arrayCopy(dst, di - position - 1, count, dst, di);
                di += count;
            }
            bit = bit << 1;
        }
        return dst;
    }

}
