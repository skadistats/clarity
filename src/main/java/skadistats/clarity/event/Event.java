package skadistats.clarity.event;


import skadistats.clarity.processor.runner.Runner;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

/**
 * The listeners of one event annotation, sorted ascending by {@link Order} (lower runs first).
 * <p>
 * The listener array is sorted once at construction and never changes. This class has no
 * {@code raise()}; it is added by the class generated for {@link GenerateEvent}, which is what
 * {@link InsertEvent} injects when the annotation has one. Raise through the annotation's nested
 * {@code Event} interface:
 * <pre>{@code
 * @InsertEvent
 * private OnEntityCreated.Event entityCreated;
 *
 * if (entityCreated.isListenedTo()) {
 *     entityCreated.raise(entity);
 * }
 * }</pre>
 *
 * @param <A> the annotation type marking the event
 *
 * @see EventListener
 * @see GenerateEvent
 * @see EventContractDiscovery
 */
public class Event<A extends Annotation> implements EventBase {

    private final Runner runner;
    private final Class<A> eventType;
    /** Listeners sorted by {@link EventListener#order}. */
    private final EventListener<A>[] listeners;

    /** Created by the runner. */
    @SuppressWarnings("unchecked")
    public Event(Runner runner, Class<A> eventType, Set<EventListener<A>> listeners) {
        this.runner = runner;
        this.eventType = eventType;
        this.listeners = listeners.toArray(new EventListener[listeners.size()]);
        Arrays.sort(this.listeners, Comparator.comparingInt(l -> l.order));
    }

    /**
     * Returns whether any listener is registered.
     * <p>
     * Providers use this to skip building event arguments when nobody listens.
     */
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
     * Passes an exception thrown by a listener to the runner's exception handler,
     * with the listener's index in the sorted array.
     */
    protected void handleListenerException(int listenerIndex, Throwable throwable) {
        runner.getExceptionHandler().handleException(eventType, new Object[] { listenerIndex }, throwable);
    }

}
