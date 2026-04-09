package skadistats.clarity.event;

import skadistats.clarity.processor.runner.Context;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class InitializerMethod extends AbstractInvocationPoint<Initializer> {

    private MethodHandle methodHandle;

    public InitializerMethod(Initializer annotation, Class<?> processorClass, Method method, UsagePointMarker marker) {
        super(annotation, processorClass, method, marker);
    }

    @Override
    public Class<? extends Annotation> getUsagePointClass() {
        return annotation.value();
    }

    @Override
    public void bind(Context ctx) throws IllegalAccessException {
        log.debug("bind %s to context", method);
        try {
            var lookup = MethodHandles.privateLookupIn(processorClass, MethodHandles.lookup());
            var handle = lookup.unreflect(method).bindTo(ctx.getProcessor(processorClass));
            var parameterTypes = method.getParameterTypes();
            if (parameterTypes.length > 0 && parameterTypes[0].isAssignableFrom(Context.class)) {
                handle = handle.bindTo(ctx);
            }
            this.methodHandle = handle.asSpreader(Object[].class, usagePointMarker.parameterClasses().length);
        } catch (IllegalAccessException e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalAccessException("binding failed for initializer " + method + ": " + t.getMessage());
        }
    }

    public void invoke(Object... args) throws Throwable {
        methodHandle.invokeExact(args);
    }

}
