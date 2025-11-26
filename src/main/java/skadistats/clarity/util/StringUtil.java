package skadistats.clarity.util;

import com.google.protobuf.ByteString;
import skadistats.clarity.io.Util;
import skadistats.clarity.source.Source;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;


public class StringUtil {

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
