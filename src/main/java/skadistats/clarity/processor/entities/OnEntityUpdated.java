package skadistats.clarity.processor.entities;

import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fires when properties of an existing, active entity were updated by a packet, and, after a
 * recreate or a reset (seek), once per entity whose state differs from before.
 *
 * <p>Handler signature: {@code void onX([Context ctx,] Entity e, FieldPath[] fps, int n)}, where
 * {@code fps} holds the changed field paths and {@code n} their count; the first {@code n} entries
 * are valid ({@code n == fps.length}, the array is a fresh copy per call). It is not raised for
 * a newly created entity; see {@link OnEntityPropertyChanged} for creation as well.
 *
 * <p>Events are raised after the whole packet has been parsed, so {@code e.getState()} already
 * reflects all changes of the packet, not only those preceding this callback. The {@link Entity}
 * instance stays the same for the entity's lifetime and its state is mutated in place by updates.
 *
 * <p>{@code classPattern}: regular expression that must match the whole DT class name
 * ({@link skadistats.clarity.model.DTClass#getDtName()}; full match, not a find). Default {@code ".*"}
 * matches all entities.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnEntityUpdated {
    /** Full-match regex against the entity's DT class name. */
    String classPattern() default ".*";

    /** Handler signature for {@link OnEntityUpdated}; implemented by the runtime, not by users. */
    interface Listener {
        void invoke(Entity e, FieldPath[] fps, int n);
    }

    /** Per-listener filter for {@link OnEntityUpdated}; built by the runtime from the annotation attributes. */
    interface Filter {
        boolean test(Entity e, FieldPath[] fps, int n);
    }

    /** Event dispatcher for {@link OnEntityUpdated}; used by the runtime. */
    interface Event extends EventBase {
        void raise(Entity e, FieldPath[] fps, int n);
    }
}
