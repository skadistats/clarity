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

@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE, ElementType.METHOD })
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
@GenerateEvent
public @interface OnModifierTableEntry {

    interface Listener {
        void invoke(DOTAModifiers.CDOTAModifierBuffTableEntry entry);
    }

    interface Event extends EventBase {
        void raise(DOTAModifiers.CDOTAModifierBuffTableEntry entry);
    }
}
