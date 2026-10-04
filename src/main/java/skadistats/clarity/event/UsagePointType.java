package skadistats.clarity.event;

import skadistats.clarity.ClarityException;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * Types of usage points in the event system.
 * <p>
 * Each usage point type represents a different kind of extension point where
 * processors can register their interest.
 *
 * @see UsagePointMarker
 * @see UsagePoint
 * @see EventListener
 */
public enum UsagePointType {

    /**
     * An event listener: a method annotated with an annotation of this type receives
     * the event. Listeners are sorted by {@link Order}.
     * <p>
     * Creates an {@link EventListener} instance.
     */
    EVENT_LISTENER,

    /**
     * An initializer: a method annotated with {@link Initializer}, called once per usage
     * point of the annotation named in {@link Initializer#value()}.
     * <p>
     * Creates an {@link InitializerMethod} instance.
     */
    INITIALIZER,

    /**
     * A feature: an annotation (typically on a class) that declares that the processor
     * needs a provider of this annotation; the runner instantiates one.
     * <p>
     * Creates a generic {@link UsagePoint} instance.
     */
    FEATURE;

    /**
     * Creates the {@link UsagePoint} subclass matching the marker on the given annotation.
     *
     * @param <A> the annotation type
     * @param annotation the usage point annotation instance
     * @param processorClass the class containing the usage point
     * @param method the method (null for class-level annotations)
     * @return a UsagePoint subclass matching this type
     * @throws ClarityException if the type is not handled
     */
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
