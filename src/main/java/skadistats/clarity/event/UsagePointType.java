package skadistats.clarity.event;

import skadistats.clarity.ClarityException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public enum UsagePointType {

    EVENT_LISTENER,
    INITIALIZER,
    FEATURE;

    public static <A extends Annotation> UsagePoint<A> newInstance(A annotation, Class<?> processorClass, Method method) {
        var marker = annotation.annotationType().getAnnotation(UsagePointMarker.class);
        switch(marker.value()) {
            case EVENT_LISTENER:
                return new EventListener(annotation, processorClass, method, marker);
            case FEATURE:
                return new UsagePoint(annotation, processorClass, method, marker);
            case INITIALIZER:
                return (UsagePoint<A>) new InitializerMethod((Initializer) annotation, processorClass, method, marker);
            default:
                throw new ClarityException("don't know how to create a newInstance for a UsagePoint of type %s", marker.value());
        }
    }

}
