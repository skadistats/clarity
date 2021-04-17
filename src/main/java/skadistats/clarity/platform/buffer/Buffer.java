package skadistats.clarity.platform.buffer;

public interface Buffer {

    byte getByte(int offs);
    int getInt(int offs);
    long getLong(int offs);

    void copy(int offs, Buffer dest, int nBytes);
    void putInt(int offs, int value);
    void putLong(int offs, long value);

}
