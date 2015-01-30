package skadistats.clarity.two.framework;

import skadistats.clarity.two.runner.Context;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class EventListener {

    private final Annotation annotation;
    private final Class<?> processorClass;
    private final Method method;

    public EventListener(Annotation annotation, Class<?> processorClass, Method method) {
        this.annotation = annotation;
        this.processorClass = processorClass;
        this.method = method;
    }

    public Class<? extends Annotation> getEventClass() {
        return annotation.annotationType();
    }

    public Class<?> getProcessorClass() {
        return processorClass;
    }

    public void invoke(Object processor, Object... params) throws InvocationTargetException, IllegalAccessException {
        method.invoke(processor, params);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EventListener that = (EventListener) o;

        if (!annotation.equals(that.annotation)) return false;
        if (!method.equals(that.method)) return false;
        if (!processorClass.equals(that.processorClass)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = annotation.hashCode();
        result = 31 * result + processorClass.hashCode();
        result = 31 * result + method.hashCode();
        return result;
    }
}
