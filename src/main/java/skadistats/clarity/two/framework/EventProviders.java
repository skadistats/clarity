package skadistats.clarity.two.framework;

import org.atteo.classindex.ClassIndex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import skadistats.clarity.two.framework.annotation.InvocationPointMarker;
import skadistats.clarity.two.framework.annotation.Provides;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.Map;

public class EventProviders {

    private static final Logger log = LoggerFactory.getLogger(EventProviders.class);

    private static Map<Class<? extends Annotation>, EventProvider> PROVIDERS = new HashMap<>();

    static {
        for (Class<?> providerClass : ClassIndex.getAnnotated(Provides.class)) {
            log.info("provider found on ClassIndex: {}", providerClass.getName());
            Provides provideAnnotation = providerClass.getAnnotation(Provides.class);
            if (provideAnnotation == null) {
                // ClassIndex does not reflect real class. Can sometimes happen when working in the IDE.
                continue;
            }
            for (Class<? extends Annotation> eventClass : provideAnnotation.value()) {
                if (!eventClass.isAnnotationPresent(InvocationPointMarker.class)) {
                    throw new RuntimeException(String.format("Class %s provides %s, which is not marked as an event.", providerClass.getName(), eventClass.getName()));
                }
                if (PROVIDERS.containsKey(eventClass)) {
                    log.warn("ignoring duplicate provider for event {} found in {}, already provided by {}", eventClass.getName(), providerClass.getName(), PROVIDERS.get(eventClass).getProviderClass().getName());
                }
                PROVIDERS.put(eventClass, new EventProvider(eventClass, providerClass));
            }
        }
    }

    public static EventProvider getEventProviderFor(Class<? extends Annotation> eventClass) {
        return PROVIDERS.get(eventClass);
    }

}
