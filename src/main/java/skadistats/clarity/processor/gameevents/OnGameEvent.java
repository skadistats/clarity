package skadistats.clarity.processor.gameevents;

import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.model.GameEvent;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fires for each {@code CSVCMsg_GameEvent} message, decoded against the descriptors from the
 * {@code CSVCMsg_GameEventList}. Events that arrive before the list are held back until it arrives; events
 * with an unknown event id are dropped with a warning.
 *
 * <p>Handler signature: {@code void onX([Context ctx,] GameEvent e)}.
 *
 * <p>{@code value()}: event name (exact match). The default {@code ""} matches all events.
 *
 * @see Listener handler signature
 * @see Filter optional filter for handlers
 * @see Event event interface
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnGameEvent {
    /** Event name; {@code ""} matches all. */
    String value() default "";

    /** Handler signature for {@link OnGameEvent}; implemented by the runtime. */
    interface Listener {
        void invoke(GameEvent e);
    }

    /** Optional filter for {@link OnGameEvent} handlers. */
    interface Filter {
        boolean test(GameEvent e);
    }

    /** Event interface for {@link OnGameEvent}; implemented by the runtime. */
    interface Event extends EventBase {
        void raise(GameEvent e);
    }
}
