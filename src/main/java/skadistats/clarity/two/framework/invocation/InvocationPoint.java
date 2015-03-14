package skadistats.clarity.two.framework.invocation;

import skadistats.clarity.two.runner.Context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface InvocationPoint<A extends Annotation> {

    A getAnnotation();
    Class<?> getProcessorClass();
    Method getMethod();

    void bind(Context ctx) throws IllegalAccessException;

    boolean isInvokedFor(Object... args);
    void invoke(Object... args) throws Throwable;


}

