package skadistats.clarity.event;


import java.lang.annotation.Annotation;
import java.util.Set;

public class Event<A extends Annotation> {

    private final Set<EventListener<A>> listeners;

    public Event(Set<EventListener<A>> listeners) {
        this.listeners = listeners;
    }

    public boolean isListenedTo() {
        return listeners.size() > 0;
    }

    public void raise(Object... args){
        for (EventListener<A> listener : listeners) {
            if (listener.isInvokedForArguments(args)) {
                try {
                    listener.invoke(args);
                } catch (Throwable throwable) {
                    throw (RuntimeException) throwable;
                }
            }
        }
    }

}
