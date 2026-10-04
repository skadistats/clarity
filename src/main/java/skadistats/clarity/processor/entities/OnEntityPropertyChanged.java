package skadistats.clarity.processor.entities;

import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.UsagePointMarker;
import skadistats.clarity.event.UsagePointType;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.processor.runner.Runner;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Fires once per changed property of an entity: for every property present when the entity is
 * created, and for every field path reported by {@link OnEntityUpdated} afterwards. It is
 * dispatched from built-in listeners of {@link OnEntityCreated} and {@link OnEntityUpdated}
 * (both {@code @Order(1000)}), so it follows those events for the same entity.
 *
 * <p>Handler signature: {@code void onX([Context ctx,] Entity e, FieldPath fp)}, where {@code fp} is
 * the changed property; read its new value with the entity's getters, and obtain its name with
 * {@link Entity#getNameForFieldPath(FieldPath)}.
 *
 * <p>Attributes:
 * <ul>
 * <li>{@code classPattern}: regular expression that must match the whole DT class name
 * ({@link skadistats.clarity.model.DTClass#getDtName()}; full match). Default {@code ".*"}.</li>
 * <li>{@code propertyPattern}: regular expression that must match the whole property name
 * ({@link Entity#getNameForFieldPath(FieldPath)}; full match). Default {@code ".*"}.</li>
 * </ul>
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
public @interface OnEntityPropertyChanged {
    /** Full-match regex against the entity's DT class name. */
    String classPattern() default ".*";
    /** Full-match regex against the property name of the changed field path. */
    String propertyPattern() default ".*";

    /** Handler signature for {@link OnEntityPropertyChanged}; implemented by the runtime, not by users. */
    interface Listener {
        void invoke(Entity e, FieldPath fp);
    }

    /** Filter signature for {@link OnEntityPropertyChanged}; the runtime applies the patterns itself. */
    interface Filter {
        boolean test(Entity e, FieldPath fp);
    }

    /** Event dispatcher for {@link OnEntityPropertyChanged}; applies the class and property patterns per listener. Used by the runtime. */
    final class Event extends skadistats.clarity.event.Event<OnEntityPropertyChanged> {
        private static final String MATCH_ALL = ".*";
        private static final Adapter[] EMPTY = new Adapter[0];

        private final Adapter[] adapters;
        private final IdentityHashMap<DTClass, Adapter[]> adaptersByClass = new IdentityHashMap<>();

        private static final class Adapter {
            final int listenerIndex;
            final Listener listener;
            final Pattern classPattern;
            final Pattern propertyPattern;
            final IdentityHashMap<DTClass, Map<FieldPath, Boolean>> propertyMatches = new IdentityHashMap<>();

            Adapter(int listenerIndex, Listener listener, OnEntityPropertyChanged annotation) {
                this.listenerIndex = listenerIndex;
                this.listener = listener;
                var cp = annotation.classPattern();
                var pp = annotation.propertyPattern();
                this.classPattern = MATCH_ALL.equals(cp) ? null : Pattern.compile(cp);
                this.propertyPattern = MATCH_ALL.equals(pp) ? null : Pattern.compile(pp);
            }

            boolean classMatches(DTClass dtClass) {
                return classPattern == null || classPattern.matcher(dtClass.getDtName()).matches();
            }

            boolean propertyMatches(Entity entity, FieldPath fp) {
                if (propertyPattern == null) return true;
                var dtClass = entity.getDtClass();
                var fpMap = propertyMatches.get(dtClass);
                if (fpMap == null) {
                    fpMap = new HashMap<>();
                    propertyMatches.put(dtClass, fpMap);
                }
                var hit = fpMap.get(fp);
                if (hit == null) {
                    hit = propertyPattern.matcher(entity.getNameForFieldPath(fp)).matches();
                    fpMap.put(fp, hit);
                }
                return hit;
            }
        }

        /** Created by the runner. */
        public Event(Runner runner, Class<OnEntityPropertyChanged> eventType, Set<EventListener<OnEntityPropertyChanged>> listeners) {
            super(runner, eventType, listeners);
            var els = listeners();
            adapters = new Adapter[els.length];
            for (int i = 0; i < els.length; i++) {
                adapters[i] = new Adapter(i, (Listener) els[i].getListenerSam(), els[i].getAnnotation());
            }
        }

        private Adapter[] adaptersFor(DTClass dtClass) {
            var arr = adaptersByClass.get(dtClass);
            if (arr == null) {
                var matching = new ArrayList<Adapter>(adapters.length);
                for (var a : adapters) {
                    if (a.classMatches(dtClass)) matching.add(a);
                }
                arr = matching.isEmpty() ? EMPTY : matching.toArray(new Adapter[0]);
                adaptersByClass.put(dtClass, arr);
            }
            return arr;
        }

        /** Invokes every listener whose class and property patterns match. */
        public void raise(Entity e, FieldPath fp) {
            var interested = adaptersFor(e.getDtClass());
            for (var a : interested) {
                if (!a.propertyMatches(e, fp)) continue;
                try {
                    a.listener.invoke(e, fp);
                } catch (Throwable t) {
                    handleListenerException(a.listenerIndex, t);
                }
            }
        }
    }
}
