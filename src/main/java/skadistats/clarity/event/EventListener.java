package skadistats.clarity.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class EventListener<A extends Annotation> extends AbstractInvocationPoint<A> {

    public EventListener(A annotation, Class<?> processorClass, Method method, UsagePointMarker marker) {
        super(annotation, processorClass, method, marker);
    }

}
