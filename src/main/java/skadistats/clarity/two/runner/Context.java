package skadistats.clarity.two.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.two.framework.EventProvider;
import skadistats.clarity.two.framework.EventProviders;
import skadistats.clarity.two.framework.annotation.InvocationPointMarker;
import skadistats.clarity.two.framework.invocation.EventListener;
import skadistats.clarity.two.framework.invocation.InvocationPoint;
import skadistats.clarity.two.framework.invocation.InvocationPointType;

import java.lang.annotation.Annotation;
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
            List<InvocationPoint> invocationPoints = findInvocationPoints(processorClass);
            for (InvocationPoint ip : invocationPoints) {
                if (ip instanceof EventListener) {
                    requireEventListener((EventListener) ip);
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

    private List<InvocationPoint> findInvocationPoints(Class<?> searchedClass) {
        List<InvocationPoint> eventListeners = new ArrayList<>();
        for (Method method : searchedClass.getMethods()) {
            for (Annotation methodAnnotation : method.getAnnotations()) {
                if (methodAnnotation.annotationType().isAnnotationPresent(InvocationPointMarker.class)) {
                    InvocationPointMarker marker = methodAnnotation.annotationType().getAnnotation(InvocationPointMarker.class);
                    InvocationPointType ipt = marker.value();
                    ipt.newInstance(methodAnnotation, searchedClass, method);
                }
            }
        }
        return eventListeners;
    }

    private void instantiateMissingProcessors() {
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

    public void initialize() {
        instantiateMissingProcessors();
        // callInitializers();
    }

    public void raise(Class<? extends Annotation> eventType, Object... params) {
    }

}
