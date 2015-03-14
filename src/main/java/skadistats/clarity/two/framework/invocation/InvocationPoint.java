package skadistats.clarity.two.framework.invocation;

import skadistats.clarity.two.processor.runner.Context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface InvocationPoint<A extends Annotation> {

    A getAnnotation();
    Class<?> getProcessorClass();
    Method getMethod();
    int getArity();

    void bind(Context ctx) throws IllegalAccessException;

    boolean isInvokedForParameterClasses(Class... classes);
    boolean isInvokedForArguments(Object... args);
    void invoke(Object... args) throws Throwable;


}

