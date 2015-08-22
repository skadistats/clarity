package skadistats.clarity.decoder;

import com.google.protobuf.ByteString;
import skadistats.clarity.processor.entities.Entities;

import java.io.UnsupportedEncodingException;

public class Util {

    public static int calcBitsNeededFor(long x) {
        if (x == 0) return (0);
        int n = 32;
        if (x <= 0x0000FFFF) {
            n = n - 16; x = x << 16;
        }
        if (x <= 0x00FFFFFF) {
            n = n - 8; x = x << 8;
        }
        if (x <= 0x0FFFFFFF) {
            n = n - 4; x = x << 4;
        }
        if (x <= 0x3FFFFFFF) {
            n = n - 2; x = x << 2;
        }
        if (x <= 0x7FFFFFFF) {
            n = n - 1;
        }
        return n;
    }

    public static int readS1EntityPropList(BitStream bs, int[] indices) {
        int i = 0;
        int cursor = -1;
        while (true) {
            if (bs.readUBitInt(1) == 1) {
                cursor += 1;
            } else {
                int offset = bs.readVarUInt();
                if (offset == Entities.MAX_PROPERTIES) {
                    return i;
                } else {
                    cursor += offset + 1;
                }
            }
            indices[i++] = cursor;
        }
    }

    public static String convertByteString(ByteString s, String charsetName) {
        try {
            return s.toString(charsetName);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

}
