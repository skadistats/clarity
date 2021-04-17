package skadistats.clarity.platform;

import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.bitstream.BitStream32;
import skadistats.clarity.io.bitstream.BitStream64;
import skadistats.clarity.platform.buffer.Buffer;
import skadistats.clarity.platform.buffer.CompatibleBuffer;
import skadistats.clarity.platform.buffer.UnsafeBuffer;

import java.lang.reflect.Method;
import java.nio.MappedByteBuffer;
import java.util.function.Consumer;
import java.util.function.Function;

public class ClarityPlatform {

    private static final boolean VM_64BIT = System.getProperty("os.arch").contains("64");
    private static final boolean VM_PRE_JAVA_9 = System.getProperty("java.specification.version","9").startsWith("1.");

    private static Function<byte[], Buffer> bufferConstructor;
    private static Function<Buffer, BitStream> bitStreamConstructor;
    private static Consumer<MappedByteBuffer> byteBufferDisposer;

    public static Function<byte[], Buffer> getBufferConstructor() {
        return bufferConstructor;
    }

    public static void setBufferConstructor(Function<byte[], Buffer> bufferConstructor) {
        ClarityPlatform.bufferConstructor = bufferConstructor;
    }

    public static Function<Buffer, BitStream> getBitStreamConstructor() {
        return bitStreamConstructor;
    }

    public static void setBitStreamConstructor(Function<Buffer, BitStream> bitStreamConstructor) {
        ClarityPlatform.bitStreamConstructor = bitStreamConstructor;
    }

    public static Consumer<MappedByteBuffer> getByteBufferDisposer() {
        return byteBufferDisposer;
    }

    public static void setByteBufferDisposer(Consumer<MappedByteBuffer> byteBufferDisposer) {
        ClarityPlatform.byteBufferDisposer = byteBufferDisposer;
    }

    public static Buffer createBuffer(byte[] data) {
        if (bufferConstructor == null) {
            bufferConstructor = determineBufferConstructor();
        }
        return bufferConstructor.apply(data);
    }

    public static BitStream createBitStream(byte[] data) {
        if (bitStreamConstructor == null) {
            bitStreamConstructor = determineBitStreamConstructor();
        }
        return bitStreamConstructor.apply(createBuffer(data));
    }

    public static void disposeMappedByteBuffer(MappedByteBuffer buf) {
        if (byteBufferDisposer == null) {
            byteBufferDisposer = determineByteBufferDisposer();
        }
        byteBufferDisposer.accept(buf);
    }

    private static Function<byte[], Buffer> determineBufferConstructor() {
        if (UnsafeBuffer.available) {
            return UnsafeBuffer::new;
        } else {
            return CompatibleBuffer::new;
        }
    }

    private static Function<Buffer, BitStream> determineBitStreamConstructor() {
        if (ClarityPlatform.VM_64BIT) {
            return BitStream64::new;
        } else {
            return BitStream32::new;
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
