package skadistats.clarity.event;


import skadistats.clarity.processor.runner.Context;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Event<A extends Annotation> {

    private final Context context;
    private final Class<A> eventType;
    private final Map<Integer, Set<EventListener<A>>> orderedListeners;

    public Event(Context context, Class<A> eventType, Set<EventListener<A>> listeners) {
        this.context = context;
        this.eventType = eventType;
        orderedListeners = new TreeMap<>();
        for (EventListener<A> listener : listeners) {
            Set<EventListener<A>> container = orderedListeners.get(listener.order);
            if (container == null) {
                container = new HashSet<>();
                orderedListeners.put(listener.order, container);
            }
            container.add(listener);
        }
    }

    public boolean isListenedTo() {
        return orderedListeners.size() > 0;
    }

    public void raise(Object... args){
        for (Set<EventListener<A>> listeners : orderedListeners.values()) {
            for (EventListener<A> listener : listeners) {
                if (listener.isInvokedForArguments(args)) {
                    try {
                        listener.invoke(args);
                    } catch (Throwable throwable) {
                        context.getExceptionHandler().handleException(eventType, args, throwable);
                    }
                }
            }
        }
    }

}
