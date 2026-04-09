package skadistats.clarity.processor.entities;

import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.runner.Runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { Entity.class, FieldPath[].class, int.class })
public @interface OnEntityUpdated {
    String classPattern() default ".*";

    interface Listener {
        void invoke(Entity e, FieldPath[] fps, int n);
    }

    interface Filter {
        boolean test(Entity e, FieldPath[] fps, int n);
    }

    final class Event extends skadistats.clarity.event.Event<OnEntityUpdated> {
        private final Listener[] typedListeners;
        private final Filter[] typedFilters;

        public Event(Runner runner, Class<OnEntityUpdated> eventType, Set<EventListener<OnEntityUpdated>> listeners) {
            super(runner, eventType, listeners);
            var els = listeners();
            typedListeners = new Listener[els.length];
            typedFilters = new Filter[els.length];
            for (int i = 0; i < els.length; i++) {
                typedListeners[i] = (Listener) els[i].getListenerSam();
                typedFilters[i] = (Filter) els[i].getFilterSam();
            }
        }

        public void raise(Entity e, FieldPath[] fps, int n) {
            for (int i = 0; i < typedListeners.length; i++) {
                var f = typedFilters[i];
                if (f != null && !f.test(e, fps, n)) continue;
                try {
                    typedListeners[i].invoke(e, fps, n);
                } catch (Throwable t) {
                    handleListenerException(i, t);
                }
            }
        }
    }
}
