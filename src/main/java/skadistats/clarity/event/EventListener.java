package skadistats.clarity.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

public class EventListener<A extends Annotation> extends AbstractInvocationPoint<A> {

    final int order;
    private Object listenerSam;
    private Object filterSam;

    public int getOrder() {
        return order;
    }

    public Object getListenerSam() {
        return listenerSam;
    }

    public void setListenerSam(Object listenerSam) {
        this.listenerSam = listenerSam;
    }

    public Object getFilterSam() {
        return filterSam;
    }

    public void setFilterSam(Object filterSam) {
        this.filterSam = filterSam;
    }

    public EventListener(A annotation, Class<?> processorClass, Method method, UsagePointMarker marker) {
        super(annotation, processorClass, method, marker);
        var ordering = method.getAnnotation(Order.class);
        order = ordering != null ? ordering.value() : 0;
    }

}
