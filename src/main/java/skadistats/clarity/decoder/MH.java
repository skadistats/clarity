package skadistats.clarity.decoder;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

public class MH {

    public static MethodHandle handle(Class<?> refc, String name, Class<?> rType, Class<?>... pTypes) {
        try {
            return MethodHandles.lookup().findStatic(refc, name, MethodType.methodType(rType, pTypes));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

}
