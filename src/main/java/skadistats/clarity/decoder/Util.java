package skadistats.clarity.decoder;

import com.google.protobuf.ByteString;
import com.rits.cloning.Cloner;

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

    public static String convertByteString(ByteString s, String charsetName) {
        try {
            return s.toString(charsetName);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    private static final Cloner CLONER = new Cloner();

    public static Object[] clone(Object[] src) {
        return CLONER.deepClone(src);
    }

    public static String arrayIdxToString(int idx) {
        return String.format("%04d", idx);
    }

}
