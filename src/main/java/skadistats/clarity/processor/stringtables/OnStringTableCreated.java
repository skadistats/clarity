package skadistats.clarity.processor.stringtables;

import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.model.StringTable;
import skadistats.clarity.processor.runner.Runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { int.class, StringTable.class })
public @interface OnStringTableCreated {

    interface Listener {
        void invoke(int numTables, StringTable table);
    }

    final class Event extends skadistats.clarity.event.Event<OnStringTableCreated> {
        private final Listener[] typedListeners;

        public Event(Runner runner, Class<OnStringTableCreated> eventType, Set<EventListener<OnStringTableCreated>> listeners) {
            super(runner, eventType, listeners);
            var els = listeners();
            typedListeners = new Listener[els.length];
            for (int i = 0; i < els.length; i++) {
                typedListeners[i] = (Listener) els[i].getListenerSam();
            }
        }

        public void raise(int numTables, StringTable table) {
            for (int i = 0; i < typedListeners.length; i++) {
                try {
                    typedListeners[i].invoke(numTables, table);
                } catch (Throwable t) {
                    handleListenerException(i, t);
                }
            }
        }
    }
}
