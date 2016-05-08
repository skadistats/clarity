package skadistats.clarity.decoder.bitstream;

import com.google.protobuf.ByteString;
import com.google.protobuf.ZeroCopy;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;

public abstract class UnsafeBitStreamBase extends BitStream {

    protected static final Unsafe unsafe;
    protected static final long base;

    static {
        try {
            Constructor<Unsafe> unsafeConstructor = Unsafe.class.getDeclaredConstructor();
            unsafeConstructor.setAccessible(true);
            unsafe = unsafeConstructor.newInstance();
            base = unsafe.arrayBaseOffset(byte[].class);
        } catch (Exception e) {
            throw (RuntimeException) e;
        }
    }

    protected final byte[] data;
    protected final long bound;

    public UnsafeBitStreamBase(ByteString input) {
        data = ZeroCopy.extract(input);
        pos = 0;
        len = data.length * 8;
        bound = ((data.length + 8) & 0xFFFFFFF8);
    }

    protected void checkAccessAbsolute(long offs, long n) {
        checkAccessRelative(offs - base, n);
    }

    protected void checkAccessRelative(long offs, long n) {
        if (offs < 0L) {
            throw new RuntimeException(
                String.format("Invalid memory access: Tried to access array of length %d at offset %d", data.length, offs)
            );
        }
        if (offs + n > bound) {
            throw new RuntimeException(
                String.format("Invalid memory access: Tried to access array of length %d at offset %d", data.length, offs + n)
            );
        }
    }
}
