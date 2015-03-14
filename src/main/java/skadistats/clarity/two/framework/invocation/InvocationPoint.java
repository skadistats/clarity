package skadistats.clarity.two.framework.invocation;

import skadistats.clarity.two.runner.Context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface InvocationPoint<A extends Annotation> {

    A getAnnotation();
    Class<?> getProcessorClass();
    Method getMethod();
    int getArity();

    void bind(Context ctx) throws IllegalAccessException;

    boolean isInvokedFor(Class... args);
    void invoke(Object... args) throws Throwable;


}

