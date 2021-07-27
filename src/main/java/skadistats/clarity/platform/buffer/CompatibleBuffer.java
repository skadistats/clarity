package skadistats.clarity.platform.buffer;

import skadistats.clarity.io.Util;

public class CompatibleBuffer {

    private CompatibleBuffer() {
        // please instantiate a subclass
    }

    public static class B32 implements Buffer.B32 {

        private final int[] data;

        public B32(byte[] input) {
            this.data = new int[(input.length + 7)  >> 2];
            Util.byteCopy(input, 0, data, 0, input.length);
        }

        @Override
        public int get(int n) {
            return data[n];
        }

    }

    public static class B64 implements Buffer.B64 {

        private final long[] data;

        public B64(byte[] input) {
            this.data = new long[(input.length + 15)  >> 3];
            Util.byteCopy(input, 0, data, 0, input.length);
        }

        @Override
        public long get(int n) {
            return data[n];
        }

    }

}
