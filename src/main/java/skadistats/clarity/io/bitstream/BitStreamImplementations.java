package skadistats.clarity.io.bitstream;

import com.google.protobuf.ByteString;
import skadistats.clarity.io.Util;

import java.lang.reflect.Constructor;

public class BitStreamImplementations {

    public static Integer implementation;

    private static final String[] bitStreamClasses = new String[] {
        "skadistats.clarity.io.bitstream.NormalBitStream32",
        "skadistats.clarity.io.bitstream.UnsafeBitStream32",
        "skadistats.clarity.io.bitstream.NormalBitStream64",
        "skadistats.clarity.io.bitstream.UnsafeBitStream64"
    };

    private static Class<?> classForName(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

    public static Constructor<BitStream> determineConstructor() {
        if (implementation == null) {
            implementation = System.getProperty("os.arch").contains("64") ? 2 : 0;
            implementation += classForName("sun.misc.Unsafe") != null ? 1 : 0;
        }
        try {
            Class<?> implClass = classForName(bitStreamClasses[implementation]);
            return (Constructor<BitStream>) implClass.getDeclaredConstructor(ByteString.class);
        } catch (Exception e) {
            Util.uncheckedThrow(e);
            return null;
        }
    }

}
