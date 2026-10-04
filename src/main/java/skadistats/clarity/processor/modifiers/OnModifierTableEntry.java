package skadistats.clarity.processor.modifiers;

import skadistats.clarity.event.EventBase;
import skadistats.clarity.event.GenerateEvent;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.wire.dota.common.proto.DOTAModifiers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Fires for each entry with data in the {@code "ActiveModifiers"} string table, decoded as a
 * {@code CDOTAModifierBuffTableEntry}.
 *
 * <p>Handler signature: {@code void onX([Context ctx,] DOTAModifiers.CDOTAModifierBuffTableEntry entry)}.
 * If parsing fails, the incomplete message is passed and the failure is logged.
 *
 * @see Listener handler signature
 * @see Event event interface
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE, ElementType.METHOD })
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnModifierTableEntry {

    /** Handler signature for {@link OnModifierTableEntry}; implemented by the runtime. */
    interface Listener {
        void invoke(DOTAModifiers.CDOTAModifierBuffTableEntry entry);
    }

    /** Event interface for {@link OnModifierTableEntry}; implemented by the runtime. */
    interface Event extends EventBase {
        void raise(DOTAModifiers.CDOTAModifierBuffTableEntry entry);
    }
}
