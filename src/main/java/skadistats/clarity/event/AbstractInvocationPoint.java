package skadistats.clarity.event;

import skadistats.clarity.processor.runner.Context;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public abstract class AbstractInvocationPoint<A extends Annotation> extends UsagePoint<A> implements InvocationPoint<A> {

    public AbstractInvocationPoint(A annotation, Class<?> processorClass, Method method, UsagePointMarker marker) {
        super(annotation, processorClass, method, marker);
    }

    public void setFilter(Object filter) {
        var contract = EventContractDiscovery.discover(annotation.annotationType());
        if (!contract.hasFilter()) {
            throw new IllegalStateException(
                    "annotation " + annotation.annotationType().getName() + " has no nested Filter SAM"
            );
        }
        if (this instanceof EventListener) {
            ((EventListener<?>) this).setFilterSam(filter);
        }
    }

    private boolean hasContextParameter() {
        var parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            return false;
        }
        return parameterTypes[0].isAssignableFrom(Context.class);
    }

    @Override
    public void bind(Context ctx) throws IllegalAccessException {
        log.debug("bind %s to context", method);
        try {
            var lookup = MethodHandles.privateLookupIn(processorClass, MethodHandles.lookup());
            var directHandle = lookup.unreflect(method);
            var processor = ctx.getProcessor(processorClass);

            if (this instanceof EventListener) {
                var contract = EventContractDiscovery.discover(annotation.annotationType());
                if (contract.hasListener()) {
                    Object[] capturedArgs;
                    if (hasContextParameter()) {
                        capturedArgs = new Object[] { processor, ctx };
                    } else {
                        capturedArgs = new Object[] { processor };
                    }
                    var sam = LmfBinder.bind(lookup, contract.listenerClass(), contract.listenerMethod(), directHandle, capturedArgs);
                    ((EventListener<?>) this).setListenerSam(sam);
                }
            }
        } catch (IllegalAccessException e) {
            throw e;
        } catch (Throwable t) {
            throw new IllegalAccessException("LMF binding failed for " + method + ": " + t.getMessage());
        }
    }

}
