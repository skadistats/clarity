package skadistats.clarity.platform.buffer;

import java.lang.invoke.MethodHandles;
import java.lang.invoke.VarHandle;
import java.nio.ByteOrder;
import java.util.Arrays;

public class VarHandleBuffer {

    private static final VarHandle INT_VIEW =
            MethodHandles.byteArrayViewVarHandle(int[].class, ByteOrder.LITTLE_ENDIAN);
    private static final VarHandle LONG_VIEW =
            MethodHandles.byteArrayViewVarHandle(long[].class, ByteOrder.LITTLE_ENDIAN);

    private VarHandleBuffer() {
    }

    private static byte[] pad(byte[] data, int wordSize) {
        // Need one extra word beyond the last data word for cross-boundary reads
        var words = (data.length + wordSize - 1) / wordSize + 1;
        var padded = new byte[words * wordSize];
        System.arraycopy(data, 0, padded, 0, data.length);
        return padded;
    }

    public static class B32 implements Buffer.B32 {
        private final byte[] data;

        public B32(byte[] data) {
            this.data = pad(data, 4);
        }

        @Override
        public int get(int n) {
            return (int) INT_VIEW.get(data, n * 4);
        }
    }

    public static class B64 implements Buffer.B64 {
        private final byte[] data;

        public B64(byte[] data) {
            this.data = pad(data, 8);
        }

        @Override
        public long get(int n) {
            return (long) LONG_VIEW.get(data, n * 8);
        }
    }
}
