package skadistats.clarity.processor.gameevents;

import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.model.GameEventDescriptor;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fires for each descriptor of a {@code CSVCMsg_GameEventList} message, which defines the name and keys of a game event type.
 *
 * <p>Handler signature: {@code void onX([Context ctx,] GameEventDescriptor e)}.
 *
 * <p>{@code value()}: descriptor name (exact match). The default {@code ""} matches all descriptors.
 *
 * @see Listener handler signature
 * @see Filter optional filter for handlers
 * @see Event event interface
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnGameEventDescriptor {
    /** Descriptor name; {@code ""} matches all. */
    String value() default "";

    /** Handler signature for {@link OnGameEventDescriptor}; implemented by the runtime. */
    interface Listener {
        void invoke(GameEventDescriptor e);
    }

    /** Optional filter for {@link OnGameEventDescriptor} handlers. */
    interface Filter {
        boolean test(GameEventDescriptor e);
    }

    /** Event interface for {@link OnGameEventDescriptor}; implemented by the runtime. */
    interface Event extends EventBase {
        void raise(GameEventDescriptor e);
    }
}
