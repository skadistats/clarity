package skadistats.clarity.processor.tempentities;

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
 * Fires for each temporary entity in a {@code CSVCMsg_TempEntities} message. Only provided for DOTA_S1 and CSGO.
 *
 * <p>Handler signature: {@code void onX([Context ctx,] Entity entity)}. The entity gets index and serial derived from
 * the engine's empty handle, and its state is read from the message.
 *
 * @see Listener handler signature
 * @see Event event interface
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnTempEntity {

    /** Handler signature for {@link OnTempEntity}; implemented by the runtime. */
    interface Listener {
        void invoke(Entity entity);
    }

    /** Event interface for {@link OnTempEntity}; implemented by the runtime. */
    interface Event extends EventBase {
        void raise(Entity entity);
    }
}
