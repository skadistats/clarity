package skadistats.clarity.event;


import skadistats.clarity.decoder.Util;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class Event<A extends Annotation> {

    private final Map<Integer, Set<EventListener<A>>> orderedListeners;

    public Event(Set<EventListener<A>> listeners) {
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
                        Util.uncheckedThrow(throwable);
                    }
                }
            }
        }
    }

}
