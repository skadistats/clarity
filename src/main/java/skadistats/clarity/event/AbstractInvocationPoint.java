package skadistats.clarity.event;

import skadistats.clarity.processor.runner.Context;
import skadistats.clarity.util.Predicate;

import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public abstract class AbstractInvocationPoint<A extends Annotation> extends UsagePoint<A> implements InvocationPoint<A> {

    protected final int arity;
    protected MethodHandle methodHandle;
    protected Class[] parameterClasses;
    protected Predicate<Object[]> invocationPredicate;

    public AbstractInvocationPoint(A annotation, Class<?> processorClass, Method method, UsagePointMarker marker) {
        super(annotation, processorClass, method, marker);
        this.arity = marker.parameterClasses().length;
        this.parameterClasses = marker.parameterClasses();
    }

    public int getArity() {
        return arity;
    }

    public void setParameterClasses(Class... classes) {
        if (classes.length != arity){
            throw new IllegalArgumentException("supplied parameter classes have wrong arity");
        }
        this.parameterClasses = classes;
    }

    public void setInvocationPredicate(Predicate<Object[]> invocationPredicate) {
        this.invocationPredicate = invocationPredicate;
    }

    private boolean hasContextParameter() {
        Class<?>[] parameterTypes = method.getParameterTypes();
        if (parameterTypes.length == 0) {
            return false;
        }
        if (parameterTypes[0].isAssignableFrom(Context.class)) {
            return true;
        }
        return false;
    }

    @Override
    public void bind(Context ctx) throws IllegalAccessException {
        log.debug("bind %s to context", method);
        MethodHandle boundHandle = MethodHandles.publicLookup().unreflect(method).bindTo(ctx.getProcessor(processorClass));
        if (hasContextParameter()) {
            boundHandle = boundHandle.bindTo(ctx);
        }
        this.methodHandle = boundHandle.asSpreader(Object[].class, arity);
    }

    @Override
    public boolean isInvokedForParameterClasses(Class... classes) throws IllegalArgumentException {
        if (classes.length != arity){
            throw new IllegalArgumentException("supplied parameter classes have wrong arity");
        }
        for (int a = 0; a < arity; a++){
            if (!parameterClasses[a].isAssignableFrom(classes[a])){
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isInvokedForArguments(Object... args) throws IllegalArgumentException {
        if (args.length != arity){
            throw new IllegalArgumentException("supplied arguments have wrong arity");
        }
        if (invocationPredicate == null) {
            return true;
        }
        return invocationPredicate.apply(args);
    }

    @Override
    public void invoke(Object... args) throws Throwable {
        methodHandle.invokeExact(args);
    }

}
