package skadistats.clarity.event;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * Creates instances of an annotation's {@code Listener} interface from a handler method
 * using {@link LambdaMetafactory}.
 * <p>
 * Used by {@link AbstractInvocationPoint#bind(Context)}.
 *
 * @see AbstractInvocationPoint
 * @see EventListener
 */
public class LmfBinder {

    /**
     * Creates an instance of {@code samClass} that captures the given arguments
     * (processor instance, optional Context) and delegates to the direct handle.
     *
     * @param lookup       a lookup with access to the target method
     * @param samClass     the interface to implement (e.g. {@code OnEntityUpdated.Listener})
     * @param samMethod    its single abstract method
     * @param directHandle the <em>direct</em> (unreflected, not bound) MethodHandle
     * @param capturedArgs the arguments to capture (processor instance, optional Context)
     * @return an instance of samClass whose method delegates to directHandle
     */
    public static Object bind(MethodHandles.Lookup lookup, Class<?> samClass, Method samMethod,
                               MethodHandle directHandle, Object[] capturedArgs) throws Throwable {
        var samMethodType = MethodType.methodType(samMethod.getReturnType(), samMethod.getParameterTypes());

        // Build the factory type from the leading parameters of the direct handle
        var handleType = directHandle.type();
        var capturedCount = capturedArgs.length;
        var capturedTypes = new Class<?>[capturedCount];
        for (int i = 0; i < capturedCount; i++) {
            capturedTypes[i] = handleType.parameterType(i);
        }
        var factoryType = MethodType.methodType(samClass, capturedTypes);

        // instantiatedMethodType = the implementation's non-captured parameter types.
        // When the user method has more specific types than the SAM (e.g. a specific
        // message class vs GeneratedMessage), LMF generates the necessary downcast.
        var instantiatedMethodType = handleType.dropParameterTypes(0, capturedCount);

        var callSite = LambdaMetafactory.metafactory(
                lookup,
                samMethod.getName(),
                factoryType,
                samMethodType,
                directHandle,
                instantiatedMethodType
        );
        return callSite.getTarget().invokeWithArguments(capturedArgs);
    }

}
