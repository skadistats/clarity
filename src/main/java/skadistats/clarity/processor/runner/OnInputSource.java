package skadistats.clarity.processor.runner;

import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.source.Source;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Raised once by a file runner after initialization, handing over the source and the loop controller. The
 * reading of the replay is driven from this event.
 *
 * <p>Handler signature: {@code void handler(Source src, LoopController ctl)}, optionally preceded by a {@link Context} parameter.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnInputSource {

    /**
     * Listener signature; implemented by the runtime.
     */
    interface Listener {
        void invoke(Source src, LoopController ctl);
    }

    /**
     * Event type; implemented by the runtime.
     */
    interface Event extends EventBase {
        void raise(Source src, LoopController ctl);
    }
}
