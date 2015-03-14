package skadistats.clarity.two.framework.invocation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public abstract class AbstractInvocationPoint<A extends Annotation> implements InvocationPoint<A> {

    protected final A annotation;
    protected final Class<?> processorClass;
    protected final Method method;

    public AbstractInvocationPoint(A annotation, Class<?> processorClass, Method method) {
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

    abstract Class<? extends Annotation> getEventClass();

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
