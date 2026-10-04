package skadistats.clarity.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Order of an event listener method among the listeners of the same event.
 * <p>
 * Listeners are sorted ascending by value, so lower values run first. The default is 0.
 * The order among listeners with equal values is not specified.
 * <p>
 * Example:
 * <pre>{@code
 * @OnEntityCreated
 * @Order(100)
 * public void late(Entity e) {
 *     // runs after listeners with order below 100
 * }
 * }</pre>
 *
 * @see EventListener#getOrder()
 */
@Target(value= ElementType.METHOD)
@Retention(value= RetentionPolicy.RUNTIME)
@Documented
public @interface Order {
    /**
     * The order value; lower runs first.
     */
    int value();
}