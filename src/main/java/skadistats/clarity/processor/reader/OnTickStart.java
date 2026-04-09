package skadistats.clarity.processor.reader;

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
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { boolean.class })
public @interface OnTickStart {

    interface Listener {
        void invoke(boolean synthetic);
    }

    final class Event extends skadistats.clarity.event.Event<OnTickStart> {
        private final Listener[] typedListeners;

        public Event(Runner runner, Class<OnTickStart> eventType, Set<EventListener<OnTickStart>> listeners) {
            super(runner, eventType, listeners);
            var els = listeners();
            typedListeners = new Listener[els.length];
            for (int i = 0; i < els.length; i++) {
                typedListeners[i] = (Listener) els[i].getListenerSam();
            }
        }

        public void raise(boolean synthetic) {
            for (int i = 0; i < typedListeners.length; i++) {
                try {
                    typedListeners[i].invoke(synthetic);
                } catch (Throwable t) {
                    handleListenerException(i, t);
                }
            }
        }
    }
}
