package skadistats.clarity.decoder.bitstream;

import com.google.protobuf.ByteString;
import com.google.protobuf.ZeroCopy;
import sun.misc.Unsafe;

import java.lang.reflect.Constructor;

public class UnsafeBitStream64 extends BitStream {

    private static final Unsafe unsafe;
    private static final long base;

    static {
        try {
            Constructor<Unsafe> unsafeConstructor = Unsafe.class.getDeclaredConstructor();
            unsafeConstructor.setAccessible(true);
            unsafe = unsafeConstructor.newInstance();
            base = unsafe.arrayBaseOffset(byte[].class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private final byte[] data;

    protected UnsafeBitStream64(ByteString input) {
        data = ZeroCopy.extract(input);
        pos = 0;
        len = data.length * 8;
    }

    protected int peekBit(int pos) {
        return (unsafe.getByte(data, base + (pos >> 3)) >> (pos & 7)) & 1;
    }

    @Override
    public int readUBitInt(int n) {
        assert n <= 32;
        int start = (pos >> 3) & 0xFFFFFFF8;
        int end = ((pos + n - 1) >> 3) & 0xFFFFFFF8;
        int s = pos & 63;
        pos += n;
        if (start == end) {
            return (int)((unsafe.getLong(data, base + start) >>> s) & MASKS[n]);
        } else { // wrap around
            return (int)(((unsafe.getLong(data, base + start) >>> s) | (unsafe.getLong(data, base + end) << (64 - s))) & MASKS[n]);
        }
    }

    @Override
    public long readUBitLong(int n) {
        assert n <= 64;
        int start = (pos >> 3) & 0xFFFFFFF8;
        int end = ((pos + n - 1) >> 3) & 0xFFFFFFF8;
        int s = pos & 63;
        pos += n;
        if (start == end) {
            return (unsafe.getLong(data, base + start) >>> s) & MASKS[n];
        } else { // wrap around
            return ((unsafe.getLong(data, base + start) >>> s) | (unsafe.getLong(data, base + end) << (64 - s))) & MASKS[n];
        }
    }

    @Override
    public byte[] readBitsAsByteArray(int n) {
        int nBytes = (n + 7) / 8;
        byte[] result = new byte[nBytes];
        if ((pos & 7) == 0) {
            unsafe.copyMemory(data, base + (pos >> 3), result, base, nBytes);
            pos += n;
            return result;
        }
        long src = base + ((pos >> 3) & 0xFFFFFFF8);
        long dst = base;
        int s = pos & 63;
        pos += n;
        long v;
        while (n >= 64) {
            v = unsafe.getLong(data, src) >>> s;
            src += 8;
            v |= unsafe.getLong(data, src) << (64 - s);
            unsafe.putLong(result, dst, v);
            dst += 8;
            n -= 64;
        }
        if (n > 0) {
            long m = MASKS[n];
            v = unsafe.getLong(data, src) >>> s;
            src += 8;
            v |= unsafe.getLong(data, src) << (64 - s);
            v &= m;
            v |= unsafe.getLong(result, dst) & ~m;
            unsafe.putLong(result, dst, v);
        }
        return result;
    }


}
