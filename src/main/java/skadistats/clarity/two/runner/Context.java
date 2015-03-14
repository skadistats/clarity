package skadistats.clarity.two.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.two.framework.EventListener;
import skadistats.clarity.two.framework.EventProvider;
import skadistats.clarity.two.framework.EventProviders;
import skadistats.clarity.two.framework.InitializerMethod;
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
    private Map<Class<? extends Annotation>, Set<InitializerMethod>> initializers = new HashMap<>();

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
                registerInitializer(new InitializerMethod(i, processorClass, initializer));
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

    private void registerInitializer(InitializerMethod initializerMethod) {
        log.info("register initializer {}", initializerMethod.getEventClass());
        Set<InitializerMethod> initializerMethods = initializers.get(initializerMethod.getEventClass());
        if (initializerMethods == null) {
            initializerMethods = new HashSet<>();
            initializers.put(initializerMethod.getEventClass(), initializerMethods);
        }
        initializerMethods.add(initializerMethod);
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

    private void callInitializers() {
        for (Map.Entry<Class<? extends Annotation>, Set<InitializerMethod>> entry : initializers.entrySet()) {
            Set<EventListener> eventListeners = processedEvents.get(entry.getKey());
            if (eventListeners != null) {
                for (EventListener eventListener : eventListeners) {
                    for (InitializerMethod method : entry.getValue()) {
                        try {
                            method.invoke(processors.get(method.getProcessorClass()), this, eventListener);
                        } catch (InvocationTargetException e) {
                            throw new RuntimeException(e);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                }
            }
        }
    }

    public void initialize() {
        instantiateMissingProcessors();
        callInitializers();
    }


    public void raise(Class<? extends Annotation> eventType, Object... params) {
        Set<EventListener> eventListeners = processedEvents.get(eventType);
        if (eventListeners == null) {
            return;
        }
        Object[] compiledParams = new Object[params.length + 1];
        compiledParams[0] = this;
        System.arraycopy(params, 0, compiledParams, 1, params.length);
        for (EventListener eventListener : eventListeners) {
            try {
                eventListener.invoke(processors.get(eventListener.getProcessorClass()), compiledParams);
            } catch (InvocationTargetException e) {
                throw new RuntimeException(eventListener.getEventClass().getName() + " failed!", e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
    }

}
