package skadistats.clarity.event;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Makes the annotation processor generate an {@link Event} subclass for an event annotation.
 * <p>
 * The generated class is named {@code <AnnotationName>_Event}, lives in the annotation's package,
 * and implements the annotation's nested {@code Event} interface. It has a
 * {@code raise(...)} method with the parameters of the {@code Listener} method, which calls the listeners in
 * order and passes exceptions thrown by a listener to the runner's exception handler. If the
 * annotation has a nested {@code Filter}, {@code raise} skips listeners whose filter
 * (if set) returns false for the arguments.
 * <p>
 * Requires a nested {@code Listener} interface (compile error otherwise) and a nested
 * {@code Event} interface that the generated class implements.
 * <p>
 * Example (see {@link skadistats.clarity.processor.entities.OnEntityCreated}):
 * <pre>{@code
 * @GenerateEvent
 * @UsagePointMarker(UsagePointType.EVENT_LISTENER)
 * public @interface OnEntityCreated {
 *     interface Listener { void invoke(Entity e); }
 *     interface Filter { boolean test(Entity e); }
 *     interface Event extends EventBase { void raise(Entity e); }
 * }
 * // generates OnEntityCreated_Event
 * }</pre>
 */
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.ANNOTATION_TYPE)
public @interface GenerateEvent {

    /**
     * Event generation strategy.
     * <p>
     * {@code STANDARD}: {@code raise()} calls all listeners in order.
     * <p>
     * {@code BUCKETED}: the annotation must have a {@code value()} of type
     * {@code Class<? extends X>}. {@code raise()} calls the listeners whose {@code value()} is
     * exactly {@code raise}'s first argument's runtime class, then the listeners whose
     * {@code value()} is {@code X} itself. Used by {@code OnMessage} and {@code OnPostEmbeddedMessage}.
     */
    Strategy strategy() default Strategy.STANDARD;

    /**
     * Available generation strategies.
     */
    enum Strategy {
        /** All listeners in order. */
        STANDARD,
        /** Dispatch by the runtime class of the first argument. */
        BUCKETED
    }

}
