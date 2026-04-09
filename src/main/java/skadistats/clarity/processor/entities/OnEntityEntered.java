package skadistats.clarity.processor.entities;

import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.model.Entity;
import skadistats.clarity.processor.runner.Runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { Entity.class })
public @interface OnEntityEntered {
    String classPattern() default ".*";

    interface Listener {
        void invoke(Entity e);
    }

    interface Filter {
        boolean test(Entity e);
    }

    final class Event extends skadistats.clarity.event.Event<OnEntityEntered> {
        private final Listener[] typedListeners;
        private final Filter[] typedFilters;

        public Event(Runner runner, Class<OnEntityEntered> eventType, Set<EventListener<OnEntityEntered>> listeners) {
            super(runner, eventType, listeners);
            var els = listeners();
            typedListeners = new Listener[els.length];
            typedFilters = new Filter[els.length];
            for (int i = 0; i < els.length; i++) {
                typedListeners[i] = (Listener) els[i].getListenerSam();
                typedFilters[i] = (Filter) els[i].getFilterSam();
            }
        }

        public void raise(Entity e) {
            for (int i = 0; i < typedListeners.length; i++) {
                var f = typedFilters[i];
                if (f != null && !f.test(e)) continue;
                try {
                    typedListeners[i].invoke(e);
                } catch (Throwable t) {
                    handleListenerException(i, t);
                }
            }
        }
    }
}
