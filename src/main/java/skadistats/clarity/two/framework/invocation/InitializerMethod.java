package skadistats.clarity.two.framework.invocation;

import skadistats.clarity.two.framework.annotation.Initializer;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class InitializerMethod extends AbstractInvocationPoint<Initializer> {

    public InitializerMethod(Initializer annotation, Class<?> processorClass, Method method) {
        super(annotation, processorClass, method);
    }

    @Override
    public Class<? extends Annotation> getEventClass() {
        return annotation.value();
    }

}
