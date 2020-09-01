package skadistats.clarity.event;


import skadistats.clarity.processor.runner.Runner;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Event<A extends Annotation> {

    private final Runner runner;
    private final Class<A> eventType;
    private final Map<Integer, Set<EventListener<A>>> orderedListeners;

    public Event(Runner runner, Class<A> eventType, Set<EventListener<A>> listeners) {
        this.runner = runner;
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
                        runner.getExceptionHandler().handleException(eventType, args, throwable);
                    }
                }
            }
        }
    }

}
