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

@Retention(RetentionPolicy.RUNTIME)
@Target(value = ElementType.METHOD)
@UsagePointMarker(value = UsagePointType.EVENT_LISTENER)
public @interface OnEntityPropertyChanged {
    String classPattern() default ".*";
    String propertyPattern() default ".*";

    interface Listener {
        void invoke(Entity e, FieldPath fp);
    }

    interface Filter {
        boolean test(Entity e, FieldPath fp);
    }

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
