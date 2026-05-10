package skadistats.clarity.platform.buffer;

public interface Buffer {
    long get(int n);
    void copyBytesInto(int srcByteOffset, byte[] dst, int dstOffset, int len);
    void put(byte[] dst, int dstOffset, long value);
}
