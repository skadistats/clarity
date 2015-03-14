package skadistats.clarity.two.framework.invocation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public interface InvocationPoint<A extends Annotation> {

    A getAnnotation();
    Class<?> getProcessorClass();
    Method getMethod();

}

