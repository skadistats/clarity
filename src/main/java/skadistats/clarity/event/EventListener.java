package skadistats.clarity.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

/**
 * A method annotated with an event annotation, together with its bound
 * {@code Listener} and {@code Filter} instances.
 * <p>
 * {@link #bind} creates the {@code Listener} instance via {@link LmfBinder}, capturing
 * the processor instance and, if the method takes one, the
 * {@link skadistats.clarity.processor.runner.Context}.
 *
 * @param <A> the event annotation type
 *
 * @see Order
 * @see LmfBinder
 * @see AbstractInvocationPoint
 */
public final class EventListener<A extends Annotation> extends AbstractInvocationPoint<A> {

    final int order;
    private Object listenerSam;
    private Object filterSam;

    /**
     * Returns the value of the method's {@link Order} annotation, or 0 if absent.
     * Lower values run first.
     */
    public int getOrder() {
        return order;
    }

    /**
     * Returns the bound {@code Listener} instance.
     *
     * @return the instance, or null if not yet bound
     */
    public Object getListenerSam() {
        return listenerSam;
    }

    /**
     * Sets the bound {@code Listener} instance. Called by {@link #bind}.
     */
    public void setListenerSam(Object listenerSam) {
        this.listenerSam = listenerSam;
    }

    /**
     * Returns the {@code Filter} set via {@link #setFilter}.
     *
     * @return the filter, or null if none was set
     */
    public Object getFilterSam() {
        return filterSam;
    }

    /**
     * Sets the {@code Filter} instance. Providers call {@link #setFilter} instead.
     */
    public void setFilterSam(Object filterSam) {
        this.filterSam = filterSam;
    }

    /** Created by the runner while scanning processors. */
    public EventListener(A annotation, Class<?> processorClass, Method method, UsagePointMarker marker) {
        super(annotation, processorClass, method, marker);
        var ordering = method.getAnnotation(Order.class);
        order = ordering != null ? ordering.value() : 0;
    }

}
