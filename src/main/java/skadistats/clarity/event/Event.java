package skadistats.clarity.event;


import skadistats.clarity.processor.runner.Runner;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

public class Event<A extends Annotation> implements EventBase {

    private final Runner runner;
    private final Class<A> eventType;
    /**
     * Listeners flattened and pre-sorted by {@link EventListener#order} at
     * construction time. Iteration over a flat array is significantly cheaper
     * than walking a TreeMap of HashSets per dispatch — and the listener set
     * never changes after Event construction, so no invalidation is needed.
     */
    private final EventListener<A>[] listeners;

    @SuppressWarnings("unchecked")
    public Event(Runner runner, Class<A> eventType, Set<EventListener<A>> listeners) {
        this.runner = runner;
        this.eventType = eventType;
        this.listeners = listeners.toArray(new EventListener[listeners.size()]);
        Arrays.sort(this.listeners, Comparator.comparingInt(l -> l.order));
    }

    public boolean isListenedTo() {
        return listeners.length > 0;
    }

    protected EventListener<A>[] listeners() {
        return listeners;
    }

    protected Runner getRunner() {
        return runner;
    }

    protected Class<A> getEventType() {
        return eventType;
    }

    /**
     * Routes a listener-thrown exception through the runner's exception
     * handler, identified by listener index.
     */
    protected void handleListenerException(int listenerIndex, Throwable throwable) {
        runner.getExceptionHandler().handleException(eventType, new Object[] { listenerIndex }, throwable);
    }

}
