package skadistats.clarity.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;

public class ClassReflector {

    private final Class<?> cls;

    public ClassReflector(String className) {
        Class<?> clsTemp;
        try {
            clsTemp = Class.forName(className);
        } catch (ClassNotFoundException e) {
            clsTemp = null;
        }
        cls = clsTemp;
    }

    public Object getDeclaredField(Object object, String name) {
        try {
            Field f = cls.getDeclaredField(name);
            f.setAccessible(true);
            return f.get(object);
        } catch (Exception e) {
            return null;
        }
    }

    public MethodHandle getPublicVirtual(String name, MethodType methodType) {
        try {
            return MethodHandles.publicLookup().findVirtual(cls, name, methodType);
        } catch (Exception e) {
            return null;
        }
    }
}
