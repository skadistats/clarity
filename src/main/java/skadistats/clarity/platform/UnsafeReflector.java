package skadistats.clarity.platform;

import skadistats.clarity.util.ClassReflector;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;

public class UnsafeReflector {

    public static final UnsafeReflector INSTANCE = new UnsafeReflector();

    private final ClassReflector reflector;
    private final Object unsafe;

    public UnsafeReflector() {
        reflector = new ClassReflector(
                "sun.misc.Unsafe",
                "jdk.internal.misc.Unsafe"
        );
        if (reflector.isValid()) {
            unsafe = reflector.getDeclaredField(null, "theUnsafe");
        } else {
            unsafe = null;
        }
    }

    public boolean isValid() {
        return reflector.isValid() && unsafe != null;
    }

    public int getByteArrayBaseOffset() {
        if (!isValid()) return 0;
        Integer bo = (Integer) reflector.getDeclaredField(null, "ARRAY_BYTE_BASE_OFFSET");
        return bo != null ? bo : 0;
    }

    public MethodHandle getPublicVirtual(String name, MethodType methodType) {
        if (!isValid()) return null;
        MethodHandle handle = reflector.getPublicVirtual(name, methodType);
        if (handle == null) return null;
        return handle.bindTo(unsafe);
    }

}
