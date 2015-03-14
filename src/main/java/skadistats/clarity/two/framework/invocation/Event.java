package skadistats.clarity.two.framework.invocation;


import java.lang.annotation.Annotation;
import java.util.Set;

public class Event<A extends Annotation> {

    private final Set<EventListener<A>> allEventHandlers;
    private Set<EventListener<A>> firingEventHandlers;

    public Event(Set<EventListener<A>> allEventHandlers) {
        this.allEventHandlers = allEventHandlers;
    }

    private void ensureFiringEventHandlersSet(){
        if (firingEventHandlers != null){
            return;
        }
        for (EventListener<A> allEventHandler : allEventHandlers) {
        }
    }

    public boolean willFire() {
        ensureFiringEventHandlersSet();
        return firingEventHandlers.size() > 0;
    }

    public void raise(Object... args){
        ensureFiringEventHandlersSet();
        for (EventListener<A> firingEventHandler : firingEventHandlers) {
            try {
                firingEventHandler.invoke(args);
            } catch (Throwable throwable) {
                throw new RuntimeException(throwable);
            }
        }
    }

}
