package skadistats.clarity.processor.reader;

import com.google.protobuf.ByteString;
import com.google.protobuf.GeneratedMessage;
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
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER, parameterClasses = { Class.class, ByteString.class })
public @interface OnMessageContainer {
    Class<? extends GeneratedMessage> value() default GeneratedMessage.class;

    interface Listener {
        void invoke(Class clazz, ByteString bytes);
    }

    interface Filter {
        boolean test(Class clazz, ByteString bytes);
    }

    final class Event extends skadistats.clarity.event.Event<OnMessageContainer> {
        private final Listener[] typedListeners;
        private final Filter[] typedFilters;

        public Event(Runner runner, Class<OnMessageContainer> eventType, Set<EventListener<OnMessageContainer>> listeners) {
            super(runner, eventType, listeners);
            var els = listeners();
            typedListeners = new Listener[els.length];
            typedFilters = new Filter[els.length];
            for (int i = 0; i < els.length; i++) {
                typedListeners[i] = (Listener) els[i].getListenerSam();
                typedFilters[i] = (Filter) els[i].getFilterSam();
            }
        }

        public void raise(Class clazz, ByteString bytes) {
            for (int i = 0; i < typedListeners.length; i++) {
                var f = typedFilters[i];
                if (f != null && !f.test(clazz, bytes)) continue;
                try {
                    typedListeners[i].invoke(clazz, bytes);
                } catch (Throwable t) {
                    handleListenerException(i, t);
                }
            }
        }
    }
}
