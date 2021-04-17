package skadistats.clarity.platform.buffer;

import skadistats.clarity.io.Util;
import skadistats.clarity.util.ClassReflector;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

public class UnsafeBuffer implements Buffer {

    private static final long base;

    private static final MethodHandle mhGetByte;
    private static final MethodHandle mhGetInt;
    private static final MethodHandle mhPutInt;
    private static final MethodHandle mhGetLong;
    private static final MethodHandle mhPutLong;

    private static final MethodHandle mhCopyMemory;

    public static final boolean available;

    static {
        ClassReflector reflector = new ClassReflector("sun.misc.Unsafe");

        Object unsafe = reflector.getDeclaredField(null, "theUnsafe");

        Integer bo = (Integer) reflector.getDeclaredField(null, "ARRAY_BYTE_BASE_OFFSET");
        base = bo != null ? bo.longValue() : 0L;

        // public int getInt(Object o, long offset)
        mhGetByte = reflector.getPublicVirtual("getByte", MethodType.methodType(byte.class, Object.class, long.class)).bindTo(unsafe);

        // public int getInt(Object o, long offset)
        mhGetInt = reflector.getPublicVirtual("getInt", MethodType.methodType(int.class, Object.class, long.class)).bindTo(unsafe);
        // public void putInt(Object o, long offset, int x)
        mhPutInt = reflector.getPublicVirtual("putInt", MethodType.methodType(void.class, Object.class, long.class, int.class)).bindTo(unsafe);

        // public long getLong(Object o, long offset)
        mhGetLong = reflector.getPublicVirtual("getLong", MethodType.methodType(long.class, Object.class, long.class)).bindTo(unsafe);
        // public void putLong(Object o, long offset, long x)
        mhPutLong = reflector.getPublicVirtual("putLong", MethodType.methodType(void.class, Object.class, long.class, long.class)).bindTo(unsafe);

        // public void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes)
        mhCopyMemory = reflector.getPublicVirtual("copyMemory", MethodType.methodType(void.class, Object.class, long.class, Object.class, long.class, long.class)).bindTo(unsafe);

        available = base != 0L
                && mhGetByte != null
                && mhGetInt != null && mhPutInt != null
                && mhGetLong != null & mhPutLong != null
                && mhCopyMemory != null;
    }


    private final byte[] data;

    public UnsafeBuffer(byte[] data) {
        this.data = data;
    }

    @Override
    public byte getByte(int offs) {
        checkBounds(offs);
        try {
            return (byte) mhGetByte.invokeExact((Object)data, base + (long)offs);
        } catch (Throwable e) {
            Util.uncheckedThrow(e);
            return 0;
        }
    }

    @Override
    public int getInt(int offs) {
        checkBounds(offs);
        try {
            return (int) mhGetInt.invokeExact((Object)data, base + (long)offs);
        } catch (Throwable e) {
            Util.uncheckedThrow(e);
            return 0;
        }
    }

    @Override
    public long getLong(int offs) {
        checkBounds(offs);
        try {
            return (long) mhGetLong.invoke((Object)data, base + (long)offs);
        } catch (Throwable e) {
            Util.uncheckedThrow(e);
            return 0L;
        }
    }

    @Override
    public void copy(int offs, Buffer dest, int nBytes) {
        UnsafeBuffer other = (UnsafeBuffer) dest;
        checkBounds(offs + nBytes);
        other.checkBounds(nBytes);
        try {
            mhCopyMemory.invokeExact((Object) data, base + (long)offs, (Object) other.data, base, (long) nBytes);
        } catch (Throwable e) {
            Util.uncheckedThrow(e);
        }
    }

    @Override
    public void putInt(int offs, int value) {
        checkBounds(offs + 4);
        try {
            mhPutInt.invokeExact((Object)data, base + (long)offs, value);
        } catch (Throwable e) {
            Util.uncheckedThrow(e);
        }
    }

    @Override
    public void putLong(int offs, long value) {
        checkBounds(offs + 8);
        try {
            mhPutLong.invokeExact((Object)data, base + (long)offs, value);
        } catch (Throwable e) {
            Util.uncheckedThrow(e);
        }
    }

    private void checkBounds(int offs) {
        if (offs < 0 || offs > data.length + 8) {
            throw new ArrayIndexOutOfBoundsException(offs);
        }
    }

}
