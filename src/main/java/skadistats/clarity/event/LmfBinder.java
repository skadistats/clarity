package skadistats.clarity.event;

import java.lang.invoke.LambdaMetafactory;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;

/**
 * Produces typed SAM instances (Listener / Filter) from a direct
 * {@link MethodHandle} via {@link LambdaMetafactory}.  The generated hidden
 * class is JIT-inlined as a direct virtual call — no {@code Object[]}
 * allocation, no {@code asSpreader} overhead.
 */
public class LmfBinder {

    /**
     * Creates a SAM instance by having LMF capture the given arguments
     * (processor instance, optional Context) and delegate to the direct handle.
     *
     * @param lookup       a lookup with access to the target method
     * @param samClass     the functional interface class (e.g. {@code OnEntityUpdated.Listener})
     * @param samMethod    the single abstract method on samClass
     * @param directHandle the <em>direct</em> (unreflected, not bound) MethodHandle
     * @param capturedArgs the arguments to capture (processor instance, optional Context)
     * @return an instance of samClass whose SAM delegates to directHandle
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
