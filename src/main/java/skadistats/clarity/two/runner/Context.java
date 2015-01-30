package skadistats.clarity.two.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.two.framework.EventListener;
import skadistats.clarity.two.framework.EventProvider;
import skadistats.clarity.two.framework.EventProviders;
import skadistats.clarity.two.framework.annotation.EventMarker;
import skadistats.clarity.two.framework.annotation.Initializer;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class Context {

    private static final Logger log = LoggerFactory.getLogger(Context.class);

    private Map<Class<?>, Object> processors = new HashMap<>();
    private Map<Class<? extends Annotation>, Set<EventListener>> processedEvents = new HashMap<>();

    public void addProcessor(Object processor) {
        requireProcessorClass(processor.getClass());
        processors.put(processor.getClass(), processor);
    }

    private void requireProcessorClass(Class<?> processorClass) {
        if (!processors.containsKey(processorClass)) {
            log.info("require processor {}", processorClass.getName());
            processors.put(processorClass, null);
            List<EventListener> consumedEventListeners = findEventListeners(processorClass);
            for (EventListener eventListener : consumedEventListeners) {
                requireEventListener(eventListener);
            }
            List<Method> initializers = findMethodsWithAnnotation(processorClass, Initializer.class);
            for (Method initializer : initializers) {
                Initializer i = initializer.getAnnotation(Initializer.class);
            }
        }
    }

    private void requireEventListener(EventListener eventListener) {
        log.info("require event listener {}", eventListener.getEventClass());
        Set<EventListener> eventListeners = processedEvents.get(eventListener.getEventClass());
        if (eventListeners == null) {
            eventListeners = new HashSet<>();
            processedEvents.put(eventListener.getEventClass(), eventListeners);
        }
        eventListeners.add(eventListener);
        EventProvider provider = EventProviders.getEventProviderFor(eventListener.getEventClass());
        if (provider == null) {
            throw new RuntimeException("oops. no provider found for required listener");
        }
        requireProcessorClass(provider.getProviderClass());
    }

    private List<EventListener> findEventListeners(Class<?> searchedClass) {
        List<EventListener> eventListeners = new ArrayList<>();
        for (Method method : searchedClass.getMethods()) {
            for (Annotation methodAnnotation : method.getAnnotations()) {
                if (methodAnnotation.annotationType().isAnnotationPresent(EventMarker.class)) {
                    eventListeners.add(new EventListener(methodAnnotation, searchedClass, method));
                }
            }
        }
        return eventListeners;
    }

    private List<Method> findMethodsWithAnnotation(Class<?> searchedClass, Class<? extends Annotation> annotationClass) {
        List<Method> methods = new ArrayList<>();
        for (Method method : searchedClass.getMethods()) {
            for (Annotation methodAnnotation : method.getAnnotations()) {
                if (methodAnnotation.annotationType() == annotationClass) {
                    methods.add(method);
                }
            }
        }
        return methods;
    }

    public void initialize() {
        for (Map.Entry<Class<?>, Object> entry : processors.entrySet()) {
            if (entry.getValue() == null) {
                try {
                    entry.setValue(entry.getKey().newInstance());
                } catch (InstantiationException e) {
                    throw new RuntimeException(e);
                } catch (IllegalAccessException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }


    public void raise(Class<? extends Annotation> eventType, Object... params) {
        Set<EventListener> eventListeners = processedEvents.get(eventType);
        if (eventListeners == null) {
            return;
        }
        Object[] compiledParams;
        if (params.length == 0) {
            compiledParams = params;
        } else {
            compiledParams = new Object[params.length + 1];
            compiledParams[0] = this;
            System.arraycopy(params, 0, compiledParams, 1, params.length);
        }
        for (EventListener eventListener : eventListeners) {
            try {
                eventListener.invoke(processors.get(eventListener.getProcessorClass()), compiledParams);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
