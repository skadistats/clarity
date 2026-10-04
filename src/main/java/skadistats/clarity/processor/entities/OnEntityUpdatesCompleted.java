package skadistats.clarity.processor.entities;

import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fires once after all entity events of a processed {@code CSVCMsg_PacketEntities} message have been
 * raised (created, entered, updated, left, deleted), and once after a reset (seek) has been applied.
 * It fires even if the packet changed nothing. A packet whose delta base has not yet been reached
 * is deferred; the event then fires when that packet is processed.
 *
 * <p>Handler signature: {@code void onX([Context ctx])}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnEntityUpdatesCompleted {

    /** Handler signature for {@link OnEntityUpdatesCompleted}; implemented by the runtime, not by users. */
    interface Listener {
        void invoke();
    }

    /** Event dispatcher for {@link OnEntityUpdatesCompleted}; used by the runtime. */
    interface Event extends EventBase {
        void raise();
    }
}
