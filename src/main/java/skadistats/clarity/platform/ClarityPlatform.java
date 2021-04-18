package skadistats.clarity.platform;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.bitstream.BitStream32;
import skadistats.clarity.io.bitstream.BitStream64;
import skadistats.clarity.platform.buffer.CompatibleBuffer;
import skadistats.clarity.platform.buffer.UnsafeBuffer;

import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.util.function.Consumer;
import java.util.function.Function;

public class ClarityPlatform {

    private static final boolean VM_64BIT = System.getProperty("os.arch").contains("64");
    private static final boolean VM_PRE_JAVA_9 = System.getProperty("java.specification.version","9").startsWith("1.");

    private static Function<byte[], BitStream> bitStreamConstructor;
    private static Consumer<MappedByteBuffer> byteBufferDisposer;

    public static Function<byte[], BitStream> getBitStreamConstructor() {
        return bitStreamConstructor;
    }

    public static void setBitStreamConstructor(Function<byte[], BitStream> bitStreamConstructor) {
        ClarityPlatform.bitStreamConstructor = bitStreamConstructor;
    }

    public static Consumer<MappedByteBuffer> getByteBufferDisposer() {
        return byteBufferDisposer;
    }

    public static void setByteBufferDisposer(Consumer<MappedByteBuffer> byteBufferDisposer) {
        ClarityPlatform.byteBufferDisposer = byteBufferDisposer;
    }

    public static BitStream createBitStream(byte[] data) {
        if (bitStreamConstructor == null) {
            synchronized (ClarityPlatform.class) {
                if (bitStreamConstructor == null) {
                    bitStreamConstructor = determineBitStreamConstructor();
                }
            }
        }
        return bitStreamConstructor.apply(data);
    }

    public static void disposeMappedByteBuffer(MappedByteBuffer buf) {
        if (byteBufferDisposer == null) {
            synchronized (ClarityPlatform.class) {
                if (byteBufferDisposer == null) {
                    byteBufferDisposer = determineByteBufferDisposer();
                }
            }
        }
        byteBufferDisposer.accept(buf);
    }

    private static Function<byte[], BitStream> determineBitStreamConstructor() {
        if (ClarityPlatform.VM_64BIT) {
            if (UnsafeBuffer.available) {
                return data -> new BitStream64(new UnsafeBuffer.B64(data));
            } else {
                return data -> new BitStream64(new CompatibleBuffer.B64(data));
            }
        } else {
            if (UnsafeBuffer.available) {
                return data -> new BitStream32(new UnsafeBuffer.B32(data));
            } else {
                return data -> new BitStream32(new CompatibleBuffer.B32(data));
            }
        }
    }

    private static Consumer<MappedByteBuffer> determineByteBufferDisposer() {
        // see http://stackoverflow.com/questions/2972986/how-to-unmap-a-file-from-memory-mapped-using-filechannel-in-java
        if (VM_PRE_JAVA_9) {
            return buf -> {
                try {
                    Method cleaner = buf.getClass().getMethod("cleaner");
                    cleaner.setAccessible(true);
                    Method clean = Class.forName("sun.misc.Cleaner").getMethod("clean");
                    clean.setAccessible(true);
                    clean.invoke(cleaner.invoke(buf));
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        } else {
            return buf -> {
                throw new UnsupportedOperationException("implement me!");
            };
        }
    }

}
