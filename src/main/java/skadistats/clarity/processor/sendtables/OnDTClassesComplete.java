package skadistats.clarity.processor.sendtables;

import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fires after the {@link skadistats.clarity.model.DTClass}es are complete: on Source 1 after {@code CDemoSyncTick} has triggered
 * flattening and superclass resolution, on Source 2 after {@code CDemoClassInfo} has created all classes and
 * assigned their ids.
 *
 * <p>Handler signature: {@code void onX([Context ctx])}.
 *
 * @see Listener handler signature
 * @see Event event interface
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnDTClassesComplete {

    /** Handler signature for {@link OnDTClassesComplete}; implemented by the runtime. */
    interface Listener {
        void invoke();
    }

    /** Event interface for {@link OnDTClassesComplete}; implemented by the runtime. */
    interface Event extends EventBase {
        void raise();
    }
}
