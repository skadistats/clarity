package skadistats.clarity.processor.reader;

import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fires at the end of each tick.
 *
 * <p>Handler signature: {@code void onX([Context ctx,] boolean synthetic)}. {@code synthetic} is {@code true}
 * for a tick number at which the replay has no data; the runner raises these to step through the gap up to the
 * next tick that has data. It is {@code false} for ticks that have data.
 *
 * @see Listener handler signature
 * @see Event event interface
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnTickEnd {

    /** Handler signature for {@link OnTickEnd}; implemented by the runtime. */
    interface Listener {
        void invoke(boolean synthetic);
    }

    /** Event interface for {@link OnTickEnd}; implemented by the runtime. */
    interface Event extends EventBase {
        void raise(boolean synthetic);
    }
}
