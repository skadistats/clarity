package skadistats.clarity.platform.buffer;

public interface Buffer {

    interface B32 {
        int get(int n);
        void copyBytesInto(int srcByteOffset, byte[] dst, int dstOffset, int len);
        void put(byte[] dst, int dstOffset, int value);
    }

    interface B64 {
        long get(int n);
        void copyBytesInto(int srcByteOffset, byte[] dst, int dstOffset, int len);
        void put(byte[] dst, int dstOffset, long value);
    }

}
