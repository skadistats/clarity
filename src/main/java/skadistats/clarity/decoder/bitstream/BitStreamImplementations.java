package skadistats.clarity.decoder.bitstream;

import com.google.protobuf.ByteString;

import java.lang.reflect.Constructor;

public class BitStreamImplementations {

    public static Integer implementation;

    private static final String[] bitStreamClasses = new String[] {
        "skadistats.clarity.decoder.bitstream.NormalBitStream32",
        "skadistats.clarity.decoder.bitstream.UnsafeBitStream32",
        "skadistats.clarity.decoder.bitstream.NormalBitStream64",
        "skadistats.clarity.decoder.bitstream.UnsafeBitStream64"
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
            implementation = System.getProperty("os.arch").indexOf("64") != -1 ? 2 : 0;
            implementation += classForName("sun.misc.Unsafe") != null ? 1 : 0;
        }
        try {
            Class<?> implClass = classForName(bitStreamClasses[implementation]);
            return (Constructor<BitStream>) implClass.getDeclaredConstructor(ByteString.class);
        } catch (Exception e) {
            throw (RuntimeException) e;
        }
    }

}
