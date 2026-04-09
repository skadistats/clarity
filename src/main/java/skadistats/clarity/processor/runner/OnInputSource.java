package skadistats.clarity.processor.runner;

import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.source.Source;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { Source.class, LoopController.class })
public @interface OnInputSource {

    interface Listener {
        void invoke(Source src, LoopController ctl);
    }

    final class Event extends skadistats.clarity.event.Event<OnInputSource> {
        private final Listener[] typedListeners;

        public Event(Runner runner, Class<OnInputSource> eventType, Set<EventListener<OnInputSource>> listeners) {
            super(runner, eventType, listeners);
            var els = listeners();
            typedListeners = new Listener[els.length];
            for (int i = 0; i < els.length; i++) {
                typedListeners[i] = (Listener) els[i].getListenerSam();
            }
        }

        public void raise(Source src, LoopController ctl) {
            for (int i = 0; i < typedListeners.length; i++) {
                try {
                    typedListeners[i].invoke(src, ctl);
                } catch (Throwable t) {
                    handleListenerException(i, t);
                }
            }
        }
    }
}
