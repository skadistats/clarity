package skadistats.clarity.processor.modifiers;

import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.processor.runner.Runner;
import skadistats.clarity.wire.dota.common.proto.DOTAModifiers;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.TYPE, ElementType.METHOD })
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { DOTAModifiers.CDOTAModifierBuffTableEntry.class })
public @interface OnModifierTableEntry {

    interface Listener {
        void invoke(DOTAModifiers.CDOTAModifierBuffTableEntry entry);
    }

    final class Event extends skadistats.clarity.event.Event<OnModifierTableEntry> {
        private final Listener[] typedListeners;

        public Event(Runner runner, Class<OnModifierTableEntry> eventType, Set<EventListener<OnModifierTableEntry>> listeners) {
            super(runner, eventType, listeners);
            var els = listeners();
            typedListeners = new Listener[els.length];
            for (int i = 0; i < els.length; i++) {
                typedListeners[i] = (Listener) els[i].getListenerSam();
            }
        }

        public void raise(DOTAModifiers.CDOTAModifierBuffTableEntry entry) {
            for (int i = 0; i < typedListeners.length; i++) {
                try {
                    typedListeners[i].invoke(entry);
                } catch (Throwable t) {
                    handleListenerException(i, t);
                }
            }
        }
    }
}
