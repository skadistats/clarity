package skadistats.clarity.processor.gameevents;

import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.model.CombatLogEntry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fires for each combat log entry collected since the previous tick end, raised at {@link skadistats.clarity.processor.reader.OnTickEnd}.
 * Entries come from the {@code dota_combatlog} game event (Source 1) or from {@code CMsgDOTACombatLogEntry}
 * messages (Source 2).
 *
 * <p>Handler signature: {@code void onX([Context ctx,] CombatLogEntry e)}.
 *
 * @see Listener handler signature
 * @see Event event interface
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnCombatLogEntry {

    /** Handler signature for {@link OnCombatLogEntry}; implemented by the runtime. */
    interface Listener {
        void invoke(CombatLogEntry e);
    }

    /** Event interface for {@link OnCombatLogEntry}; implemented by the runtime. */
    interface Event extends EventBase {
        void raise(CombatLogEntry e);
    }
}
