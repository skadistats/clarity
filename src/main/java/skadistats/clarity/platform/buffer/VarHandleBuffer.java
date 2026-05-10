package skadistats.clarity.platform.buffer;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;

public class VarHandleBuffer implements Buffer {

    private static final VarHandle LONG_VIEW =
            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    private final byte[] data;

    public VarHandleBuffer(byte[] data) {
        // one extra long beyond the last data word for cross-boundary reads
        var padded = new byte[((data.length + 7) / 8 + 1) * 8];
        System.arraycopy(data, 0, padded, 0, data.length);
        this.data = padded;
    }

    @Override
    public long get(int n) {
        return (long) LONG_VIEW.get(data, n * 8);
    }

    @Override
    public void copyBytesInto(int srcByteOffset, byte[] dst, int dstOffset, int len) {
        System.arraycopy(data, srcByteOffset, dst, dstOffset, len);
    }

    @Override
    public void put(byte[] dst, int dstOffset, long value) {
        LONG_VIEW.set(dst, dstOffset, value);
    }
}
