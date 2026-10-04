package skadistats.clarity.processor.sendtables;

import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.model.DTClass;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fires for each {@link DTClass} as it is created: for each {@code CSVCMsg_SendTable} on Source 1, for each
 * entry of {@code CDemoClassInfo} on Source 2. At this point the class id is not yet assigned, and on
 * Source 1 the properties are not yet flattened.
 *
 * <p>Handler signature: {@code void onX([Context ctx,] DTClass dtClass)}.
 *
 * @see Listener handler signature
 * @see Event event interface
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnDTClass {

    /** Handler signature for {@link OnDTClass}; implemented by the runtime. */
    interface Listener {
        void invoke(DTClass dtClass);
    }

    /** Event interface for {@link OnDTClass}; implemented by the runtime. */
    interface Event extends EventBase {
        void raise(DTClass dtClass);
    }
}
