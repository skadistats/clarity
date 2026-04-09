package skadistats.clarity.processor.entities;

import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.processor.runner.Runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { })
public @interface OnEntityUpdatesCompleted {

    interface Listener {
        void invoke();
    }

    final class Event extends skadistats.clarity.event.Event<OnEntityUpdatesCompleted> {
        private final Listener[] typedListeners;

        public Event(Runner runner, Class<OnEntityUpdatesCompleted> eventType, Set<EventListener<OnEntityUpdatesCompleted>> listeners) {
            super(runner, eventType, listeners);
            var els = listeners();
            typedListeners = new Listener[els.length];
            for (int i = 0; i < els.length; i++) {
                typedListeners[i] = (Listener) els[i].getListenerSam();
            }
        }

        public void raise() {
            for (int i = 0; i < typedListeners.length; i++) {
                try {
                    typedListeners[i].invoke();
                } catch (Throwable t) {
                    handleListenerException(i, t);
                }
            }
        }
    }
}
