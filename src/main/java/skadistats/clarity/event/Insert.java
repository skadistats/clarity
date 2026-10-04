package skadistats.clarity.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field of a processor for injection.
 * <ul>
 *   <li>If the field type is {@link skadistats.clarity.processor.runner.Context} (or a supertype), the runner's context is injected.</li>
 *   <li>Otherwise, the first processor instance assignable to the field type is injected.
 *       If none exists, a {@link skadistats.clarity.ClarityException} is thrown.</li>
 * </ul>
 * <p>
 * Example:
 * <pre>{@code
 * public class MyProcessor {
 *     @Insert
 *     private Context context;
 *
 *     @Insert
 *     private SomeOtherProcessor other;
 * }
 * }</pre>
 *
 * @see InsertEvent
 */
@Target(value= ElementType.FIELD)
@Retention(value= RetentionPolicy.RUNTIME)
@Documented
public @interface Insert {
}