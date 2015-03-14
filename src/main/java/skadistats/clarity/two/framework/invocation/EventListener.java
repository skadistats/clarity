package skadistats.clarity.two.framework.invocation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class EventListener extends AbstractInvocationPoint<Annotation> {

    public EventListener(Annotation annotation, Class<?> processorClass, Method method) {
        super(annotation, processorClass, method);
    }

    public Class<? extends Annotation> getEventClass() {
        return annotation.annotationType();
    }

}
