package skadistats.clarity.processor.runner;

import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Raised once after the processors have been wired and before any replay data is read.
 *
 * <p>Handler signature: {@code void handler()}, optionally preceded by a {@link Context} parameter.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnInit {

    /**
     * Listener signature; implemented by the runtime.
     */
    interface Listener {
        void invoke();
    }

    /**
     * Event type; implemented by the runtime.
     */
    interface Event extends EventBase {
        void raise();
    }
}
