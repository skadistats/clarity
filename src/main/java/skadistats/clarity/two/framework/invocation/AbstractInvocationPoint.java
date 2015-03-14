package skadistats.clarity.two.framework.invocation;

import skadistats.clarity.two.runner.Context;

import java.lang.annotation.Annotation;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public abstract class AbstractInvocationPoint<A extends Annotation> extends UsagePoint<A> implements InvocationPoint<A> {

    protected final int arity;
    protected MethodHandle methodHandle;
    protected Class[] parameterClasses;

    public AbstractInvocationPoint(A annotation, Class<?> processorClass, Method method, int arity) {
        super(annotation, processorClass, method);
        this.arity = arity;
        this.parameterClasses = new Class[arity];
        for (int a = 0; a < arity; a++){
            parameterClasses[a] = Object.class;
        }
    }

    public int getArity() {
        return arity;
    }

    public void setParameterClasses(Class... parameterClasses) {
        this.parameterClasses = parameterClasses;
    }

    @Override
    public void bind(Context ctx) throws IllegalAccessException {
        log.info("bind {} to context", method);
        MethodHandle boundHandle = MethodHandles.publicLookup().unreflect(method).bindTo(ctx.getProcessor(processorClass)).bindTo(ctx);
        methodHandle = new ConstantCallSite(boundHandle).dynamicInvoker();
    }

    @Override
    public boolean isInvokedFor(Class... classes) throws IllegalArgumentException {
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
    public void invoke(Object... args) throws Throwable {
        methodHandle.invokeWithArguments(args);
    }

}
