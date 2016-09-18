package skadistats.clarity.processor.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.decoder.Util;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.InitializerMethod;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.InvocationPoint;
import skadistats.clarity.event.Insert;
import skadistats.clarity.event.Provides;
import skadistats.clarity.event.UsagePoint;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointProvider;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.event.UsagePoints;
import skadistats.clarity.model.EngineType;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExecutionModel {

    private static final Logger log = LoggerFactory.getLogger(ExecutionModel.class);

    private final Runner runner;
    private final Map<Class<?>, Object> processors = new HashMap<>();
    private final Set<UsagePoint> usagePoints = new HashSet<>();
    private final Map<Class<? extends Annotation>, Set<EventListener>> processedEvents = new HashMap<>();
    private final Map<Class<? extends Annotation>, InitializerMethod> initializers = new HashMap<>();

    public ExecutionModel(Runner runner) {
        this.runner = runner;
        addProcessor(runner);
    }

    public void addProcessor(Object processor) {
        requireProcessorClass(processor.getClass());
        processors.put(processor.getClass(), processor);
    }

    public <T> T getProcessor(Class<T> processorClass) {
        //noinspection unchecked
        return (T) processors.get(processorClass);
    }

    public Runner getRunner() {
        return runner;
    }

    private boolean hasProcessorForClass(Class<?> processorClass) {
        for (Class<?> existingClass : processors.keySet()) {
            if (processorClass.isAssignableFrom(existingClass)) {
                return true;
            }
        }
        return false;
    }

    private void requireProcessorClass(Class<?> processorClass) {
        if (!hasProcessorForClass(processorClass)) {
            log.debug("require processor {}", processorClass.getName());
            processors.put(processorClass, null);
            List<UsagePoint<? extends Annotation>> ups = findUsagePoints(processorClass);
            for (UsagePoint<? extends Annotation> up : ups) {
                usagePoints.add(up);
                if (up instanceof EventListener) {
                    requireEventListener((EventListener) up);
                } else if (up instanceof InitializerMethod) {
                    registerInitializer((InitializerMethod) up);
                } else {
                    requireProvider(up);
                }
            }
        }
    }

    private boolean supportsRunner(Provides provides) {
        for (Class<? extends Runner> supportedRunner : provides.runnerClass()) {
            if (supportedRunner.isAssignableFrom(runner.getClass())) {
                return true;
            }
        }
        return false;
    }

    private boolean supportsEngineType(Provides provides) {
        for (EngineType engineType : provides.engine()) {
            if (engineType == runner.getEngineType()) {
                return true;
            }
        }
        return false;
    }

    private void requireProvider(UsagePoint<? extends Annotation> up) {
        Class<? extends Annotation> usagePointClass = up.getUsagePointClass();
        List<UsagePointProvider> providers = UsagePoints.getProvidersFor(usagePointClass);
        if (providers != null) {
            for (UsagePointProvider usagePointProvider : providers) {
                Provides a = usagePointProvider.getProvidesAnnotation();
                if (a.runnerClass().length > 0 && !supportsRunner(a)) {
                    continue;
                }
                if (a.engine().length > 0 && !supportsEngineType(a)) {
                    continue;
                }
                requireProcessorClass(usagePointProvider.getProviderClass());
                return;
            }
        }
        throw new RuntimeException("oops. no provider found for required usage point " + usagePointClass);
    }


    private void requireEventListener(EventListener eventListener) {
        log.debug("require event listener {}", eventListener.getUsagePointClass());
        Set<EventListener> eventListeners = processedEvents.get(eventListener.getUsagePointClass());
        if (eventListeners == null) {
            eventListeners = new HashSet<>();
            processedEvents.put(eventListener.getUsagePointClass(), eventListeners);
        }
        eventListeners.add(eventListener);
        requireProvider(eventListener);
    }

    private void registerInitializer(InitializerMethod initializer) {
        log.debug("register initializer {}", initializer.getUsagePointClass());
        if (initializers.containsKey(initializer.getUsagePointClass())) {
            log.warn("ignoring duplicate initializer for event {} found in {}, already provided by {}", initializer.getUsagePointClass().getName(), initializer.getProcessorClass().getName(), initializers.get(initializer.getUsagePointClass()).getProcessorClass().getName());
            return;
        }
        initializers.put(initializer.getUsagePointClass(), initializer);
    }

    private List<UsagePoint<? extends Annotation>> findUsagePoints(Class<?> searchedClass) {
        List<UsagePoint<? extends Annotation>> ups = new ArrayList<>();
        for (Annotation classAnnotation : searchedClass.getAnnotations()) {
            if (classAnnotation.annotationType().isAnnotationPresent(UsagePointMarker.class)) {
                ups.add(UsagePointType.newInstance(classAnnotation, searchedClass, null));
            }
        }
        Class<?> c = searchedClass;
        while (true) {
            for (Method method : c.getDeclaredMethods()) {
                for (Annotation methodAnnotation : method.getAnnotations()) {
                    if (methodAnnotation.annotationType().isAnnotationPresent(UsagePointMarker.class)) {
                        method.setAccessible(true);
                        ups.add(UsagePointType.newInstance(methodAnnotation, searchedClass, method));
                    }
                }
            }
            c = c.getSuperclass();
            if (c == Object.class) {
                break;
            }
        }
        return ups;
    }

    private void instantiateMissingProcessors() {
        for (Map.Entry<Class<?>, Object> entry : processors.entrySet()) {
            if (entry.getValue() == null) {
                try {
                    entry.setValue(entry.getKey().newInstance());
                } catch (Exception e) {
                    throw Util.toRuntimeException(e);
                }
            }
        }
    }

    private void bindInvocationPoints(skadistats.clarity.processor.runner.Context context) {
        for (UsagePoint up : usagePoints) {
            if (up instanceof InvocationPoint){
                try {
                    ((InvocationPoint)up).bind(context);
                } catch (IllegalAccessException e) {
                    throw Util.toRuntimeException(e);
                }

            }
        }
    }

    private void processInjections() {
        for (Object processor : processors.values()) {
            Class<?> c = processor.getClass();
            while (true) {
                for (Field field : c.getDeclaredFields()) {
                    for (Annotation fieldAnnotation : field.getAnnotations()) {
                        if (fieldAnnotation instanceof Insert) {
                            if (field.getType().isAssignableFrom(Context.class)) {
                                injectValue(processor, field, runner.getContext(), "cannot inject context");
                            } else if (field.getType().isAssignableFrom(EngineType.class)) {
                                injectValue(processor, field, runner.getContext().getEngineType(), "cannot inject engine type");
                            } else {
                                injectProcessor(processor, field);
                            }
                        } else if (fieldAnnotation instanceof InsertEvent) {
                            injectEvent(processor, field, (InsertEvent) fieldAnnotation);
                        }
                    }
                }
                c = c.getSuperclass();
                if (c == Object.class) {
                    break;
                }
            }
        }
    }

    private void injectEvent(Object processor, Field field, InsertEvent fieldAnnotation) {
        Class<? extends Annotation> eventType = (Class<? extends Annotation>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        Class<?>[] parameterTypes;
        if (fieldAnnotation.override()) {
            parameterTypes = fieldAnnotation.parameterTypes();
        } else {
            UsagePointMarker marker = eventType.getAnnotation(UsagePointMarker.class);
            parameterTypes = marker.parameterClasses();
        }
        Set<EventListener<Annotation>> listeners = computeListenersForEvent((Class<Annotation>) eventType, parameterTypes);
        Event<?> injectedValue = null;
        if (listeners.size() != 0) {
            injectedValue = new Event<>(listeners);
        }
        injectValue(processor, field, injectedValue, "cannot inject event");
    }

    private void injectProcessor(Object processor, Field field) {
        Object injectedValue = null;
        for (Object p : processors.values()) {
            if (field.getType().isAssignableFrom(p.getClass())) {
                injectedValue = p;
                break;
            }
        }
        if (injectedValue == null) {
            throw new RuntimeException(String.format(
                    "cannot inject processor of type %s into processor of type %s: not found!",
                    field.getType().getName(),
                    processor.getClass().getName()
            ));
        }
        injectValue(processor, field, injectedValue, "cannot inject processor");
    }

    private void injectValue(Object processor, Field field, Object value, String errMessage) {
        try {
            field.setAccessible(true);
            field.set(processor, value);
        } catch (Exception e) {
            throw new RuntimeException(
                    String.format(
                            "%s, field is class %s, value is class %s",
                            errMessage,
                            field.getClass().getName(),
                            runner.getContext().getClass().getName()
                    ),
                    e
            );
        }
    }

    private void callInitializers() {
        for (UsagePoint up : usagePoints) {
            if (up instanceof InitializerMethod) {
                continue;
            }
            InitializerMethod im = initializers.get(up.getUsagePointClass());
            if (im != null){
                try {
                    im.invoke(up);
                } catch (Throwable e) {
                    throw Util.toRuntimeException(e);
                }
            }
        }
    }

    public void initialize(skadistats.clarity.processor.runner.Context context) {
        instantiateMissingProcessors();
        bindInvocationPoints(context);
        processInjections();
        callInitializers();
    }

    public <A extends Annotation> Event<A> createEvent(Class<A> eventType, Class... parameterTypes) {
        Set<EventListener<A>> listeners = computeListenersForEvent(eventType, parameterTypes);
        return new Event<>(listeners);
    }

    private <A extends Annotation> Set<EventListener<A>> computeListenersForEvent(Class<A> eventType, Class... parameterTypes) {
        Set<EventListener<A>> listeners = new HashSet<>();
        Set<EventListener> eventListeners = processedEvents.get(eventType);
        if(eventListeners != null) {
            for (@SuppressWarnings("unchecked") EventListener<A> listener : eventListeners) {
                if (listener.isInvokedForParameterClasses(parameterTypes)) {
                    listeners.add(listener);
                }
            }
        }
        return listeners;
    }

}
