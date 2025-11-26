package skadistats.clarity.processor.runner;

import org.slf4j.Logger;
import skadistats.clarity.ClarityException;
import skadistats.clarity.LogChannel;
import skadistats.clarity.io.Util;
import skadistats.clarity.event.Event;
import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.InitializerMethod;
import skadistats.clarity.event.Insert;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.InvocationPoint;
import skadistats.clarity.event.Provides;
import skadistats.clarity.event.UsagePoint;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointProvider;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.event.UsagePoints;
import skadistats.clarity.logger.PrintfLoggerFactory;
import skadistats.clarity.model.EngineId;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ExecutionModel {

    private static final Logger log = PrintfLoggerFactory.getLogger(LogChannel.executionModel);

    private final Runner runner;
    private final Map<Class<?>, Object> processors = new HashMap<>();
    private final Set<UsagePoint> usagePoints = new HashSet<>();
    private final Map<Class<? extends Annotation>, Set<EventListener>> processedEvents = new HashMap<>();
    private final Map<Class<? extends Annotation>, InitializerMethod> initializers = new HashMap<>();

    public ExecutionModel(Runner runner) {
        this.runner = runner;
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
        for (var existingClass : processors.keySet()) {
            if (processorClass.isAssignableFrom(existingClass)) {
                return true;
            }
        }
        return false;
    }

    private void requireProcessorClass(Class<?> processorClass) {
        if (!hasProcessorForClass(processorClass)) {
            log.debug("require processor %s", processorClass.getName());
            processors.put(processorClass, null);
            var ups = findUsagePoints(processorClass);
            for (var up : ups) {
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
        for (var supportedRunner : provides.runnerClass()) {
            if (supportedRunner.isAssignableFrom(runner.getClass())) {
                return true;
            }
        }
        return false;
    }

    private boolean supportsEngineType(Provides provides) {
        for (var id : provides.engine()) {
            if (id == runner.getEngineType().getId()) {
                return true;
            }
        }
        return false;
    }

    private void requireProvider(UsagePoint<? extends Annotation> up) {
        var usagePointClass = up.getUsagePointClass();
        var providers = UsagePoints.getProvidersFor(usagePointClass);
        if (providers != null) {
            for (var usagePointProvider : providers) {
                var providesAnnotation = usagePointProvider.getProvidesAnnotation();
                if (isInvalidProvider(providesAnnotation)) continue;
                if (hasProcessorForClass(usagePointProvider.getProviderClass())) return;
            }
            for (var usagePointProvider : providers) {
                var providesAnnotation = usagePointProvider.getProvidesAnnotation();
                if (isInvalidProvider(providesAnnotation)) continue;
                requireProcessorClass(usagePointProvider.getProviderClass());
                return;
            }
        }
        throw new ClarityException("oops. no provider found for required usage point %s", usagePointClass);
    }

    private boolean isInvalidProvider(Provides a) {
        if (a.runnerClass().length > 0 && !supportsRunner(a)) {
            return true;
        }
        if (a.engine().length > 0 && !supportsEngineType(a)) {
            return true;
        }
        return false;
    }


    private void requireEventListener(EventListener eventListener) {
        log.debug("require event listener %s", eventListener.getUsagePointClass());
        var eventListeners = processedEvents.get(eventListener.getUsagePointClass());
        if (eventListeners == null) {
            eventListeners = new HashSet<>();
            processedEvents.put(eventListener.getUsagePointClass(), eventListeners);
        }
        eventListeners.add(eventListener);
        requireProvider(eventListener);
    }

    private void registerInitializer(InitializerMethod initializer) {
        log.debug("register initializer %s", initializer.getUsagePointClass());
        if (initializers.containsKey(initializer.getUsagePointClass())) {
            log.warn("ignoring duplicate initializer for event %s found in %s, already provided by %s", initializer.getUsagePointClass().getName(), initializer.getProcessorClass().getName(), initializers.get(initializer.getUsagePointClass()).getProcessorClass().getName());
            return;
        }
        initializers.put(initializer.getUsagePointClass(), initializer);
    }

    private List<UsagePoint<? extends Annotation>> findUsagePoints(Class<?> searchedClass) {
        List<UsagePoint<? extends Annotation>> ups = new ArrayList<>();
        collectClassAnnotationUsagePoints(searchedClass, ups);
        collectMethodAnnotationUsagePoints(searchedClass, ups);
        return ups;
    }

    private void collectClassAnnotationUsagePoints(Class<?> searchedClass, List<UsagePoint<? extends Annotation>> ups) {
        Class<?> c = searchedClass;
        while (c != Object.class) {
            for (var classAnnotation : c.getAnnotations()) {
                if (classAnnotation.annotationType().isAnnotationPresent(UsagePointMarker.class)) {
                    ups.add(UsagePointType.newInstance(classAnnotation, searchedClass, null));
                }
            }
            c = c.getSuperclass();
        }
    }

    private void collectMethodAnnotationUsagePoints(Class<?> searchedClass, List<UsagePoint<? extends Annotation>> ups) {
        Class<?> c = searchedClass;
        while (c != Object.class) {
            for (var method : c.getDeclaredMethods()) {
                for (var methodAnnotation : method.getAnnotations()) {
                    if (methodAnnotation.annotationType().isAnnotationPresent(UsagePointMarker.class)) {
                        method.setAccessible(true);
                        ups.add(UsagePointType.newInstance(methodAnnotation, searchedClass, method));
                    }
                }
            }
            c = c.getSuperclass();
        }
    }

    private void instantiateMissingProcessors() {
        for (var entry : processors.entrySet()) {
            if (entry.getValue() == null) {
                try {
                    entry.setValue(entry.getKey().newInstance());
                } catch (Exception e) {
                    Util.uncheckedThrow(e);
                }
            }
        }
    }

    private void bindInvocationPoints(skadistats.clarity.processor.runner.Context context) {
        for (var up : usagePoints) {
            if (up instanceof InvocationPoint) {
                try {
                    ((InvocationPoint) up).bind(context);
                } catch (IllegalAccessException e) {
                    Util.uncheckedThrow(e);
                }
            }
        }
    }

    private void processInjections() {
        for (var processor : processors.values()) {
            var c = processor.getClass();
            while (true) {
                for (var field : c.getDeclaredFields()) {
                    for (var fieldAnnotation : field.getAnnotations()) {
                        if (fieldAnnotation instanceof Insert) {
                            if (field.getType().isAssignableFrom(Context.class)) {
                                injectValue(processor, field, runner.getContext(), "cannot inject context");
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
        var eventType = (Class<? extends Annotation>) ((ParameterizedType) field.getGenericType()).getActualTypeArguments()[0];
        Class<?>[] parameterTypes;
        if (fieldAnnotation.override()) {
            parameterTypes = fieldAnnotation.parameterTypes();
        } else {
            var marker = eventType.getAnnotation(UsagePointMarker.class);
            parameterTypes = marker.parameterClasses();
        }
        injectValue(processor, field, createEvent(eventType, parameterTypes), "cannot inject event");
    }

    private void injectProcessor(Object processor, Field field) {
        Object injectedValue = null;
        for (var p : processors.values()) {
            if (field.getType().isAssignableFrom(p.getClass())) {
                injectedValue = p;
                break;
            }
        }
        if (injectedValue == null) {
            throw new ClarityException(
                    "cannot inject processor of type %s into processor of type %s: not found!",
                    field.getType().getName(),
                    processor.getClass().getName()
            );
        }
        injectValue(processor, field, injectedValue, "cannot inject processor");
    }

    private void injectValue(Object processor, Field field, Object value, String errMessage) {
        try {
            field.setAccessible(true);
            field.set(processor, value);
        } catch (Exception e) {
            throw new ClarityException(
                    e,
                    "%s, field is class %s, value is class %s",
                    errMessage,
                    field.getClass().getName(),
                    runner.getContext().getClass().getName()
            );
        }
    }

    private void callInitializers() {
        for (var up : usagePoints) {
            if (up instanceof InitializerMethod) {
                continue;
            }
            var im = initializers.get(up.getUsagePointClass());
            if (im != null) {
                try {
                    im.invoke(up);
                } catch (Throwable e) {
                    Util.uncheckedThrow(e);
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
        var listeners = computeListenersForEvent(eventType, parameterTypes);
        return new Event<>(runner, eventType, listeners);
    }

    private <A extends Annotation> Set<EventListener<A>> computeListenersForEvent(Class<A> eventType, Class... parameterTypes) {
        Set<EventListener<A>> listeners = new HashSet<>();
        var eventListeners = processedEvents.get(eventType);
        if (eventListeners != null) {
            for (@SuppressWarnings("unchecked") EventListener<A> listener : eventListeners) {
                if (listener.isInvokedForParameterClasses(parameterTypes)) {
                    listeners.add(listener);
                }
            }
        }
        return listeners;
    }

}
