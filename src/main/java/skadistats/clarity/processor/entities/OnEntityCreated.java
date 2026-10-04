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
 * Fires when an entity comes into existence: for a new entity in a packet, and, after a reset
 * (seek), for entities that did not exist before it.
 *
 * <p>Handler signature: {@code void onX([Context ctx,] Entity e)}, where {@code e} is the new entity.
 * Its state is fully populated (baseline plus creation data). It is followed immediately by
 * {@link OnEntityEntered} if the entity is active. When a new entity replaces a different one in
 * the same index slot, {@link OnEntityLeft} (if active) and {@link OnEntityDeleted} of the old entity fire first.
 * The same {@link Entity} instance is passed to all later events for this entity.
 *
 * <p>{@code classPattern}: regular expression that must match the whole DT class name
 * ({@link skadistats.clarity.model.DTClass#getDtName()}; full match, not a find). Default {@code ".*"}
 * matches all entities.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnEntityCreated {
    /** Full-match regex against the entity's DT class name. */
    String classPattern() default ".*";

    /** Handler signature for {@link OnEntityCreated}; implemented by the runtime, not by users. */
    interface Listener {
        void invoke(Entity e);
    }

    /** Per-listener filter for {@link OnEntityCreated}; built by the runtime from the annotation attributes. */
    interface Filter {
        boolean test(Entity e);
    }

    /** Event dispatcher for {@link OnEntityCreated}; used by the runtime. */
    interface Event extends EventBase {
        void raise(Entity e);
    }
}
