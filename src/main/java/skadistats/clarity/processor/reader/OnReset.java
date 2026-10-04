package skadistats.clarity.processor.reader;

import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.wire.shared.demo.proto.Demo;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fires in phases while the runner resets its state to seek to another tick (see {@link ResetPhase},
 * raised in that order). Only {@link skadistats.clarity.processor.runner.ControllableRunner} seeks.
 *
 * <p>Handler signature: {@code void onX([Context ctx,] Demo.CDemoStringTables packet, ResetPhase phase)}.
 * {@code packet} is non-null only in {@link ResetPhase#ACCUMULATE}, where it holds the string tables of one
 * reset-relevant packet; otherwise it is {@code null}.
 *
 * @see ResetPhase
 * @see Listener handler signature
 * @see Event event interface
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnReset {

    /** Handler signature for {@link OnReset}; implemented by the runtime. */
    interface Listener {
        void invoke(Demo.CDemoStringTables packet, ResetPhase phase);
    }

    /** Event interface for {@link OnReset}; implemented by the runtime. */
    interface Event extends EventBase {
        void raise(Demo.CDemoStringTables packet, ResetPhase phase);
    }
}
