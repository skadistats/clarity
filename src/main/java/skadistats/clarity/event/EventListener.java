package skadistats.clarity.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class EventListener<A extends Annotation> extends AbstractInvocationPoint<A> {

    final int order;

    public EventListener(A annotation, Class<?> processorClass, Method method, UsagePointMarker marker) {
        super(annotation, processorClass, method, marker);
        Order ordering = method.getAnnotation(Order.class);
        order = ordering != null ? ordering.value() : 0;
    }

}
