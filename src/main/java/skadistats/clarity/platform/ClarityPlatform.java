package skadistats.clarity.platform;

import org.slf4j.Logger;
import skadistats.clarity.LogChannel;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.bitstream.BitStream32;
import skadistats.clarity.io.bitstream.BitStream64;
import skadistats.clarity.logger.PrintfLoggerFactory;
import skadistats.clarity.platform.buffer.CompatibleBuffer;
import skadistats.clarity.platform.buffer.UnsafeBuffer;
import skadistats.clarity.util.ClassReflector;
import skadistats.clarity.util.ThrowingRunnable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.util.function.Consumer;
import java.util.function.Function;

public class ClarityPlatform {

    private static final Logger log = PrintfLoggerFactory.getLogger(LogChannel.runner);

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
            ClassReflector cleanerReflector = new ClassReflector("sun.misc.Cleaner");
            // public void clean()
            MethodHandle mhClean = cleanerReflector.getPublicVirtual(
                    "clean", MethodType.methodType(void.class));

            ClassReflector bufReflector = new ClassReflector("sun.nio.ch.DirectBuffer");
            // public Cleaner cleaner()
            MethodHandle mhCleaner = bufReflector.getPublicVirtual(
                    "cleaner", MethodType.methodType(cleanerReflector.getCls()));

            return buf -> runCleaner(
                    () -> mhClean.invoke(mhCleaner.invoke(buf)),
                    mhClean, mhCleaner
            );
        } else {
            MethodHandle mhInvokeCleaner = UnsafeReflector.INSTANCE.getPublicVirtual(
                    "invokeCleaner",
                    MethodType.methodType(void.class, ByteBuffer.class));

            return buf -> runCleaner(
                    () -> mhInvokeCleaner.invoke(buf),
                    mhInvokeCleaner
            );
        }
    }

    private static void runCleaner(ThrowingRunnable runnable, Object... nonNulls) {
        for (Object nonNull : nonNulls) {
            if (nonNull == null) {
                log.error("Cannot run cleaner because method was not found. Please file an issue!");
                return;
            }
        }
        try {
            runnable.run();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

}
