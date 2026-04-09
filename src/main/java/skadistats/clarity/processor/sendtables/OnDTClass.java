package skadistats.clarity.processor.sendtables;

import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.processor.runner.Runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { DTClass.class })
public @interface OnDTClass {

    interface Listener {
        void invoke(DTClass dtClass);
    }

    final class Event extends skadistats.clarity.event.Event<OnDTClass> {
        private final Listener[] typedListeners;

        public Event(Runner runner, Class<OnDTClass> eventType, Set<EventListener<OnDTClass>> listeners) {
            super(runner, eventType, listeners);
            var els = listeners();
            typedListeners = new Listener[els.length];
            for (int i = 0; i < els.length; i++) {
                typedListeners[i] = (Listener) els[i].getListenerSam();
            }
        }

        public void raise(DTClass dtClass) {
            for (int i = 0; i < typedListeners.length; i++) {
                try {
                    typedListeners[i].invoke(dtClass);
                } catch (Throwable t) {
                    handleListenerException(i, t);
                }
            }
        }
    }
}
