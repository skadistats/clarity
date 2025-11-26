package skadistats.clarity.io;

import org.xerial.snappy.Snappy;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.util.StringUtil;
import skadistats.clarity.source.Source;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;

public class Util {

    public static int calcBitsNeededFor(long x) {
        if (x == 0) return (0);
        var n = 32;
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

    public static String readFixedZeroTerminated(Source source, int length) throws IOException {
        var buf = new byte[length];
        source.readBytes(buf, 0, length);
        return StringUtil.zeroTerminatedToString(buf);
    }

    public static <T> Class<T> valueClassForDecoder(Decoder<T> decoder) {
        var interfaceType = (ParameterizedType) decoder.getClass().getGenericInterfaces()[0];
        return (Class<T>)interfaceType.getActualTypeArguments()[0];
    }

    public static void byteCopy(Object src, int srcOffset, Object dst, int dstOffset, int n) {
        try {
            Snappy.arrayCopy(src, srcOffset, n, dst, dstOffset);
        } catch (IOException e) {
            Util.uncheckedThrow(e);
        }
    }

    public static void uncheckedThrow(Throwable e) {
        Util.<RuntimeException>uncheckedThrow0(e);
    }

    @SuppressWarnings("unchecked")
    private static <E extends Throwable> void uncheckedThrow0(Throwable e) throws E {
        throw (E) e;
    }

}
