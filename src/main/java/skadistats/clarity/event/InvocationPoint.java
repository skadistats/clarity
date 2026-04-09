package skadistats.clarity.event;

import skadistats.clarity.processor.runner.Context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface InvocationPoint<A extends Annotation> {

    A getAnnotation();
    Class<?> getProcessorClass();
    Method getMethod();

    void bind(Context ctx) throws IllegalAccessException;

}
