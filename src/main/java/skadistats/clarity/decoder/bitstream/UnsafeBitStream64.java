package skadistats.clarity.decoder.bitstream;

import com.google.protobuf.ByteString;
import com.google.protobuf.ZeroCopy;
import skadistats.clarity.decoder.s2.FieldOpHuffmanTree;
import skadistats.clarity.decoder.s2.FieldOpType;
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
    public void readBitsIntoByteArray(byte[] dest, int n) {
        int nBytes = (n + 7) / 8;
        if ((pos & 7) == 0) {
            unsafe.copyMemory(data, base + (pos >> 3), dest, base, nBytes);
            pos += n;
            return;
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
            unsafe.putLong(dest, dst, v);
            dst += 8;
            n -= 64;
        }
        if (n > 0) {
            long m = MASKS[n];
            v = unsafe.getLong(data, src) >>> s;
            src += 8;
            v |= unsafe.getLong(data, src) << (64 - s);
            v &= m;
            v |= unsafe.getLong(dest, dst) & ~m;
            unsafe.putLong(dest, dst, v);
        }
    }

    @Override
    public FieldOpType readFieldOp() {
        long offs = base + ((pos >> 3) & 0xFFFFFFF8);
        long v = unsafe.getLong(data, offs);
        long s = 1L << (pos & 63);
        int i = 0;
        while (true) {
            pos++;
            i = FieldOpHuffmanTree.tree[i][(v & s) != 0 ? 1 : 0];
            if (i < 0) {
                return FieldOpHuffmanTree.ops[-i - 1];
            }
            s = s << 1;
            if (s == 0) {
                offs += 8;
                v = unsafe.getLong(data, offs);
                s = 1;
            }
        }
    }

}
