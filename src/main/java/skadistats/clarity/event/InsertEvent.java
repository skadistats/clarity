package skadistats.clarity.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field of a processor for injection of an event instance.
 * <p>
 * The event annotation is determined from the field type:
 * <ul>
 *   <li>a type nested in an annotation (e.g. {@code OnEntityCreated.Event}): that annotation;</li>
 *   <li>a subclass of {@link Event}: its enclosing class;</li>
 *   <li>otherwise the type argument of the generic field type (e.g. {@code Event<OnEntityCreated>}).</li>
 * </ul>
 * <p>
 * The injected instance holds all listeners registered for that annotation. It has
 * {@code raise(...)} only if declared through the nested {@code Event} interface, so use that
 * form for raising.
 * <p>
 * Example:
 * <pre>{@code
 * public class MyProcessor {
 *     @InsertEvent
 *     private Event<OnEntityCreated> entityCreated;
 *
 *     @InsertEvent
 *     private OnEntityDeleted.Event entityDeleted;
 * }
 * }</pre>
 *
 * @see Insert
 * @see Event
 * @see skadistats.clarity.processor.runner.ExecutionModel#createEvent(Class)
 */
@Target(value= ElementType.FIELD)
@Retention(value= RetentionPolicy.RUNTIME)
@Documented
public @interface InsertEvent {
}
