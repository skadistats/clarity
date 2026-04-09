package skadistats.clarity.processor.stringtables;

import com.google.protobuf.ByteString;
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
@Target(value = { ElementType.TYPE, ElementType.METHOD })
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { StringTable.class, int.class, String.class, ByteString.class })
public @interface OnStringTableEntry {
    String value();

    interface Listener {
        void invoke(StringTable table, int index, String key, ByteString value);
    }

    interface Filter {
        boolean test(StringTable table, int index, String key, ByteString value);
    }

    final class Event extends skadistats.clarity.event.Event<OnStringTableEntry> {
        private final Listener[] typedListeners;
        private final Filter[] typedFilters;

        public Event(Runner runner, Class<OnStringTableEntry> eventType, Set<EventListener<OnStringTableEntry>> listeners) {
            super(runner, eventType, listeners);
            var els = listeners();
            typedListeners = new Listener[els.length];
            typedFilters = new Filter[els.length];
            for (int i = 0; i < els.length; i++) {
                typedListeners[i] = (Listener) els[i].getListenerSam();
                typedFilters[i] = (Filter) els[i].getFilterSam();
            }
        }

        public void raise(StringTable table, int index, String key, ByteString value) {
            for (int i = 0; i < typedListeners.length; i++) {
                var f = typedFilters[i];
                if (f != null && !f.test(table, index, key, value)) continue;
                try {
                    typedListeners[i].invoke(table, index, key, value);
                } catch (Throwable t) {
                    handleListenerException(i, t);
                }
            }
        }
    }
}
