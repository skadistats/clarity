package skadistats.clarity.io;

import com.google.protobuf.ByteString;
import org.xerial.snappy.Snappy;
import skadistats.clarity.io.decoder.Decoder;
import skadistats.clarity.source.Source;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.ParameterizedType;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

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

    public static String convertByteString(ByteString s, String charsetName) {
        try {
            return s.toString(charsetName);
        } catch (UnsupportedEncodingException e) {
            Util.uncheckedThrow(e);
            return null;
        }
    }

    public static String readFixedZeroTerminated(ByteBuffer buffer, int length) {
        var buf = new byte[length];
        buffer.get(buf);
        return zeroTerminatedToString(buf);
    }

    public static String readFixedZeroTerminated(Source source, int length) throws IOException {
        var buf = new byte[length];
        source.readBytes(buf, 0, length);
        return zeroTerminatedToString(buf);
    }

    public static String zeroTerminatedToString(byte[] buf) {
        var i = 0;
        while (buf[i] != 0) i++;
        return new String(buf, 0, i, StandardCharsets.UTF_8);
    }

    public static String arrayIdxToString(int idx) {
        var sb = new StringBuilder(4);
        if (idx < 10) {
            sb.append("000");
        } else if (idx < 100) {
            sb.append("00");
        } else if (idx < 1000) {
            sb.append("0");
        }
        sb.append(idx);
        return sb.toString();
    }

    public static int stringToArrayIdx(String value) {
        return Integer.parseInt(value);
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

    public static String formatHexDump(byte[] array, int offset, int length) {
        var builder = new StringBuilder();
        for (var rowOffset = offset; rowOffset < offset + length; rowOffset += 16) {
            builder.append(String.format("%06d:  ", rowOffset));
            for (var index = 0; index < 16; index++) {
                if (rowOffset + index < array.length) {
                    builder.append(String.format("%02x ", array[rowOffset + index]));
                } else {
                    break;
                }
            }
            builder.append(String.format("%n"));
        }
        return builder.toString();
    }

}
