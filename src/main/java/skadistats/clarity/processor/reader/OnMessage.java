package skadistats.clarity.processor.reader;

import com.google.protobuf.GeneratedMessage;
import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.processor.runner.Runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { GeneratedMessage.class }, dynamicParameters = true)
public @interface OnMessage {
    Class<? extends GeneratedMessage> value() default GeneratedMessage.class;

    interface Listener {
        void invoke(GeneratedMessage msg);
    }

    final class Event extends skadistats.clarity.event.Event<OnMessage> {
        private static final Entry[] EMPTY = new Entry[0];

        private final Map<Class<? extends GeneratedMessage>, Entry[]> byClass;
        private final Entry[] wildcardEntries;

        private static final class Entry {
            final int listenerIndex;
            final Listener listener;
            Entry(int listenerIndex, Listener listener) {
                this.listenerIndex = listenerIndex;
                this.listener = listener;
            }
        }

        public Event(Runner runner, Class<OnMessage> eventType, Set<EventListener<OnMessage>> listeners) {
            super(runner, eventType, listeners);
            var els = listeners();

            Map<Class<? extends GeneratedMessage>, List<Entry>> buckets = new HashMap<>();
            List<Entry> wildcards = new ArrayList<>();

            for (int i = 0; i < els.length; i++) {
                var msgClass = els[i].getAnnotation().value();
                var entry = new Entry(i, (Listener) els[i].getListenerSam());
                if (msgClass == GeneratedMessage.class) {
                    wildcards.add(entry);
                } else {
                    buckets.computeIfAbsent(msgClass, k -> new ArrayList<>()).add(entry);
                }
            }

            this.byClass = new HashMap<>();
            for (var e : buckets.entrySet()) {
                this.byClass.put(e.getKey(), e.getValue().toArray(EMPTY));
            }
            this.wildcardEntries = wildcards.toArray(EMPTY);
        }

        public boolean isListenedTo(Class<? extends GeneratedMessage> messageClass) {
            return byClass.containsKey(messageClass) || wildcardEntries.length > 0;
        }

        public void raise(GeneratedMessage msg) {
            var bucket = byClass.get(msg.getClass());
            if (bucket != null) {
                for (var e : bucket) {
                    try {
                        e.listener.invoke(msg);
                    } catch (Throwable t) {
                        handleListenerException(e.listenerIndex, t);
                    }
                }
            }
            for (var e : wildcardEntries) {
                try {
                    e.listener.invoke(msg);
                } catch (Throwable t) {
                    handleListenerException(e.listenerIndex, t);
                }
            }
        }
    }
}
