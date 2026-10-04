package skadistats.clarity.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation that marks an annotation as a usage point of the given {@link UsagePointType}.
 * <p>
 * The execution model scans processor classes (and superclasses) for class and method
 * annotations that carry this marker.
 * <p>
 * Example:
 * <pre>{@code
 * @Retention(RetentionPolicy.RUNTIME)
 * @Target(ElementType.METHOD)
 * @UsagePointMarker(UsagePointType.EVENT_LISTENER)
 * @GenerateEvent
 * public @interface MyCustomEvent {
 *     interface Listener { void invoke(SomeData data); }
 *     interface Event extends EventBase { void raise(SomeData data); }
 * }
 * }</pre>
 *
 * @see UsagePointType
 * @see EventListener
 * @see skadistats.clarity.event.UsagePoints
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = {ElementType.ANNOTATION_TYPE})
public @interface UsagePointMarker {
    /**
     * The type of this usage point.
     *
     * @see UsagePointType
     */
    UsagePointType value();

    /**
     * If true, the compile-time listener validation checks only the number of handler parameters
     * against the nested {@code Listener}, not their types (handler parameters may be subtypes,
     * e.g. a concrete message class for {@code OnMessage}). Without it, each parameter type must
     * be assignable in one direction to the {@code Listener} parameter type.
     */
    boolean dynamicParameters() default false;
}
