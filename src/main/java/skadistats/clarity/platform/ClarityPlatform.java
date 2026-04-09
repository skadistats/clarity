package skadistats.clarity.platform;

import org.slf4j.Logger;
import skadistats.clarity.LogChannel;
import skadistats.clarity.io.bitstream.BitStream;
import skadistats.clarity.io.bitstream.BitStream32;
import skadistats.clarity.io.bitstream.BitStream64;
import skadistats.clarity.logger.PrintfLoggerFactory;
import skadistats.clarity.platform.buffer.VarHandleBuffer;

import java.nio.MappedByteBuffer;
import java.util.function.Consumer;
import java.util.function.Function;

public class ClarityPlatform {

    private static final Logger log = PrintfLoggerFactory.getLogger(LogChannel.runner);

    private static final boolean VM_64BIT = System.getProperty("os.arch").contains("64");

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
            return data -> new BitStream64(new VarHandleBuffer.B64(data));
        } else {
            return data -> new BitStream32(new VarHandleBuffer.B32(data));
        }
    }

    private static Consumer<MappedByteBuffer> determineByteBufferDisposer() {
        // see http://stackoverflow.com/questions/2972986/how-to-unmap-a-file-from-memory-mapped-using-filechannel-in-java
        try {
            var unsafeClass = Class.forName("sun.misc.Unsafe");
            var theUnsafe = unsafeClass.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            var unsafe = theUnsafe.get(null);
            var invokeCleaner = unsafeClass.getMethod("invokeCleaner", java.nio.ByteBuffer.class);
            return buf -> {
                try {
                    invokeCleaner.invoke(unsafe, buf);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        } catch (Exception e) {
            log.error("Cannot set up MappedByteBuffer disposer. Please file an issue!");
            return buf -> {};
        }
    }

}
