package skadistats.clarity.event;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Discovers the nested typed contract (Listener, Filter, Event) inside an
 * {@code @UsagePointMarker(EVENT_LISTENER)}-bearing annotation, following the
 * naming convention established by the typed-event-dispatch migration.
 */
public class EventContractDiscovery {

    private static final ConcurrentHashMap<Class<? extends Annotation>, Contract> CACHE = new ConcurrentHashMap<>();

    public static Contract discover(Class<? extends Annotation> annotationType) {
        return CACHE.computeIfAbsent(annotationType, EventContractDiscovery::doDiscover);
    }

    @SuppressWarnings("unchecked")
    private static Contract doDiscover(Class<? extends Annotation> annotationType) {
        Class<?> listenerClass = null;
        Method listenerMethod = null;
        Class<?> filterClass = null;
        Method filterMethod = null;
        Class<? extends Event> eventClass = null;

        for (var nested : annotationType.getDeclaredClasses()) {
            var name = nested.getSimpleName();
            if (nested.isInterface() && "Listener".equals(name)) {
                listenerClass = nested;
                listenerMethod = findSam(nested);
            } else if (nested.isInterface() && "Filter".equals(name)) {
                filterClass = nested;
                filterMethod = findSam(nested);
            } else if (!nested.isInterface() && "Event".equals(name) && Event.class.isAssignableFrom(nested)) {
                eventClass = (Class<? extends Event>) nested;
            }
        }

        return new Contract(listenerClass, listenerMethod, filterClass, filterMethod, eventClass);
    }

    private static Method findSam(Class<?> iface) {
        for (var m : iface.getMethods()) {
            if (java.lang.reflect.Modifier.isAbstract(m.getModifiers())) {
                return m;
            }
        }
        return null;
    }

    public record Contract(
            Class<?> listenerClass,
            Method listenerMethod,
            Class<?> filterClass,
            Method filterMethod,
            Class<? extends Event> eventClass
    ) {
        public boolean hasListener() {
            return listenerClass != null;
        }

        public boolean hasFilter() {
            return filterClass != null;
        }

        public boolean hasEventClass() {
            return eventClass != null;
        }
    }

}
