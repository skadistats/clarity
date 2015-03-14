package skadistats.clarity.two.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.two.framework.EventProvider;
import skadistats.clarity.two.framework.EventProviders;
import skadistats.clarity.two.framework.annotation.InvocationPointMarker;
import skadistats.clarity.two.framework.invocation.*;
import skadistats.clarity.two.framework.invocation.EventListener;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.*;

public class Context {

    private static final Logger log = LoggerFactory.getLogger(Context.class);

    private Map<Class<?>, Object> processors = new HashMap<>();
    private Set<InvocationPoint> invocationPoints = new HashSet<>();
    private Map<Class<? extends Annotation>, Set<EventListener>> processedEvents = new HashMap<>();
    private Map<Class<? extends Annotation>, InitializerMethod> initializers = new HashMap<>();

    public void addProcessor(Object processor) {
        requireProcessorClass(processor.getClass());
        processors.put(processor.getClass(), processor);
    }

    public <T> T getProcessor(Class<T> processorClass) {
        return (T) processors.get(processorClass);
    }

    private void requireProcessorClass(Class<?> processorClass) {
        if (!processors.containsKey(processorClass)) {
            log.info("require processor {}", processorClass.getName());
            processors.put(processorClass, null);
            List<InvocationPoint> ips = findInvocationPoints(processorClass);
            for (InvocationPoint ip : ips) {
                invocationPoints.add(ip);
                if (ip instanceof EventListener) {
                    requireEventListener((EventListener) ip);
                } else if (ip instanceof InitializerMethod) {
                    registerInitializer((InitializerMethod) ip);
                }
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

    private void registerInitializer(InitializerMethod initializer) {
        log.info("register initializer {}", initializer.getEventClass());
        if (initializers.containsKey(initializer.getEventClass())) {
            log.warn("ignoring duplicate initializer for event {} found in {}, already provided by {}", initializer.getEventClass().getName(), initializer.getProcessorClass().getName(), initializers.get(initializer.getEventClass()).getProcessorClass().getName());
            return;
        }
        initializers.put(initializer.getEventClass(), initializer);
    }

    private List<InvocationPoint> findInvocationPoints(Class<?> searchedClass) {
        List<InvocationPoint> invocationPoints = new ArrayList<>();
        for (Method method : searchedClass.getMethods()) {
            for (Annotation methodAnnotation : method.getAnnotations()) {
                if (methodAnnotation.annotationType().isAnnotationPresent(InvocationPointMarker.class)) {
                    InvocationPointMarker marker = methodAnnotation.annotationType().getAnnotation(InvocationPointMarker.class);
                    InvocationPointType ipt = marker.value();
                    invocationPoints.add(ipt.newInstance(methodAnnotation, searchedClass, method, marker.arity()));
                }
            }
        }
        return invocationPoints;
    }

    private void instantiateMissingProcessors() {
        for (Map.Entry<Class<?>, Object> entry : processors.entrySet()) {
            if (entry.getValue() == null) {
                try {
                    entry.setValue(entry.getKey().newInstance());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private void bindInvocationPoints() {
        for (InvocationPoint invocationPoint : invocationPoints) {
            try {
                invocationPoint.bind(this);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void callInitializers() {
        for (Map.Entry<Class<? extends Annotation>, InitializerMethod> initializerMethodEntry : initializers.entrySet()) {
            Set<EventListener> eventListeners = processedEvents.get(initializerMethodEntry.getKey());
            if (eventListeners != null) {
                for (EventListener eventListener : eventListeners) {
                    try {
                        initializerMethodEntry.getValue().invoke(eventListener);
                    } catch (Throwable e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    public void initialize() {
        instantiateMissingProcessors();
        bindInvocationPoints();
        callInitializers();
    }

    public <A extends Annotation> Event<A> createEvent(Class<A> eventType, Class... parameterTypes) {
        Set<EventListener<A>> listeners = new HashSet<>();
        Set<EventListener> eventListeners = processedEvents.get(eventType);
        if(eventListeners != null) {
            for (EventListener<A> listener : eventListeners) {
                if (listener.isInvokedFor(parameterTypes)) {
                    listeners.add(listener);
                }
            }
        }
        return new Event<>(listeners);
    }

}
