package skadistats.clarity.two.framework.invocation;

import com.google.common.base.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.two.runner.Context;

import java.lang.annotation.Annotation;
import java.lang.invoke.ConstantCallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public abstract class AbstractInvocationPoint<A extends Annotation> implements InvocationPoint<A> {

    protected final Logger log;

    protected final A annotation;
    protected final Class<?> processorClass;
    protected final Method method;
    protected MethodHandle methodHandle;
    protected Predicate<Class[]> invocationClassPredicate;
    protected Predicate<Object[]> invocationParameterPredicate;

    public AbstractInvocationPoint(A annotation, Class<?> processorClass, Method method) {
        this.log = LoggerFactory.getLogger(getClass());
        this.annotation = annotation;
        this.processorClass = processorClass;
        this.method = method;
    }

    public A getAnnotation() {
        return annotation;
    }

    public Class<?> getProcessorClass() {
        return processorClass;
    }

    public Method getMethod() {
        return method;
    }

    public Predicate<Object[]> getInvocationParameterPredicate() {
        return invocationParameterPredicate;
    }

    public void setInvocationParameterPredicate(Predicate<Object[]> invocationParameterPredicate) {
        this.invocationParameterPredicate = invocationParameterPredicate;
    }

    abstract Class<? extends Annotation> getEventClass();

    public void bind(Context ctx) throws IllegalAccessException {
        log.info("bind {} to context", method);
        MethodHandle boundHandle = MethodHandles.publicLookup().unreflect(method).bindTo(ctx.getProcessor(processorClass)).bindTo(ctx);
        methodHandle = new ConstantCallSite(boundHandle).dynamicInvoker();
    }

    public boolean isInvokedFor(Object... args) {
        return invocationParameterPredicate == null ? true : invocationParameterPredicate.apply(args);
    }

    public void invoke(Object... args) throws Throwable {
        methodHandle.invokeWithArguments(args);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AbstractInvocationPoint that = (AbstractInvocationPoint) o;
        if (!annotation.equals(that.annotation)) return false;
        if (!method.equals(that.method)) return false;
        if (!processorClass.equals(that.processorClass)) return false;
        return true;
    }

    @Override
    public int hashCode() {
        int result = annotation.hashCode();
        result = 31 * result + processorClass.hashCode();
        result = 31 * result + method.hashCode();
        return result;
    }

}
