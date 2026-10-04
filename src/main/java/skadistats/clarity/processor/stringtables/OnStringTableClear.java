package skadistats.clarity.processor.stringtables;

import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fires when all string tables are cleared. Raised only on Source 2 engines, on {@code CSVCMsg_ClearAllStringTables}.
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
public @interface OnStringTableClear {

    /** Handler signature for {@link OnStringTableClear}; implemented by the runtime. */
    interface Listener {
        void invoke();
    }

    /** Event interface for {@link OnStringTableClear}; implemented by the runtime. */
    interface Event extends EventBase {
        void raise();
    }
}
