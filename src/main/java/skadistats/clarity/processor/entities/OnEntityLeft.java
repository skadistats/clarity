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
 * Fires when an active entity becomes inactive (leaves the client's view) without ceasing to exist,
 * and as the first step of its removal: before {@link OnEntityDeleted}, and before the replacement
 * entity is created when an index slot is reused.
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
public @interface OnEntityLeft {
    /** Full-match regex against the entity's DT class name. */
    String classPattern() default ".*";

    /** Handler signature for {@link OnEntityLeft}; implemented by the runtime, not by users. */
    interface Listener {
        void invoke(Entity e);
    }

    /** Per-listener filter for {@link OnEntityLeft}; built by the runtime from the annotation attributes. */
    interface Filter {
        boolean test(Entity e);
    }

    /** Event dispatcher for {@link OnEntityLeft}; used by the runtime. */
    interface Event extends EventBase {
        void raise(Entity e);
    }
}
