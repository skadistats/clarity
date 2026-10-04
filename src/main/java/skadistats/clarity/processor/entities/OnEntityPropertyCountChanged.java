package skadistats.clarity.processor.entities;

import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.model.Entity;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fires when the number of properties of an entity changed, i.e. properties were added or removed
 * (for example when a variable-size array grows or shrinks). It fires before the accompanying
 * {@link OnEntityUpdated} of the same update.
 *
 * <p>Handler signature: {@code void onX([Context ctx,] Entity e)}.
 *
 * <p>{@code classPattern}: regular expression that must match the whole DT class name
 * ({@link skadistats.clarity.model.DTClass#getDtName()}; full match, not a find). Default {@code ".*"}
 * matches all entities.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnEntityPropertyCountChanged {
    /** Full-match regex against the entity's DT class name. */
    String classPattern() default ".*";

    /** Handler signature for {@link OnEntityPropertyCountChanged}; implemented by the runtime, not by users. */
    interface Listener {
        void invoke(Entity e);
    }

    /** Per-listener filter for {@link OnEntityPropertyCountChanged}; built by the runtime from the annotation attributes. */
    interface Filter {
        boolean test(Entity e);
    }

    /** Event dispatcher for {@link OnEntityPropertyCountChanged}; used by the runtime. */
    interface Event extends EventBase {
        void raise(Entity e);
    }
}
