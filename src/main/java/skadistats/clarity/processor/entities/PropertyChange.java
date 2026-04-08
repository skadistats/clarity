package skadistats.clarity.processor.entities;

import skadistats.clarity.event.Event;
import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.Initializer;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Order;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Dispatcher for {@link OnEntityPropertyChanged} listeners.
 *
 * <p>Bypasses the standard {@code Event.raise} dispatch path so we can:
 * <ul>
 *     <li>cache class- and property-pattern match results per
 *         {@link DTClass} (results depend only on the class, not on the
 *         entity instance, so the cache survives entity deletions);</li>
 *     <li>pre-filter the list of listeners interested in a given
 *         {@link DTClass} once per entity update, instead of once per
 *         {@link FieldPath} per listener;</li>
 *     <li>skip the predicate machinery entirely for listeners with
 *         wildcard patterns ({@code .*}).</li>
 * </ul>
 */
@Provides({OnEntityPropertyChanged.class})
public class PropertyChange {

    private static final String MATCH_ALL = ".*";

    /**
     * Injected only so we can route listener-thrown exceptions through the
     * runner's exception handler. We never call {@code raise()} on it — all
     * dispatch happens in {@link #dispatch} below.
     */
    @InsertEvent
    private Event<OnEntityPropertyChanged> evPropertyChanged;

    private final List<ListenerAdapter> adapters = new ArrayList<>();
    /** Per-DTClass list of adapters whose classPattern matches that class. */
    private final IdentityHashMap<DTClass, ListenerAdapter[]> adaptersByClass = new IdentityHashMap<>();

    private static final class ListenerAdapter {
        final EventListener<OnEntityPropertyChanged> listener;
        final Pattern classPattern;     // null if matches all
        final Pattern propertyPattern;  // null if matches all
        /** Cached property-name match results per DTClass (only used when propertyPattern != null). */
        final IdentityHashMap<DTClass, Map<FieldPath, Boolean>> propertyMatches = new IdentityHashMap<>();

        ListenerAdapter(EventListener<OnEntityPropertyChanged> listener) {
            this.listener = listener;
            var classPat = listener.getAnnotation().classPattern();
            var propPat = listener.getAnnotation().propertyPattern();
            this.classPattern = MATCH_ALL.equals(classPat) ? null : Pattern.compile(classPat);
            this.propertyPattern = MATCH_ALL.equals(propPat) ? null : Pattern.compile(propPat);
        }

        boolean classMatches(DTClass dtClass) {
            return classPattern == null || classPattern.matcher(dtClass.getDtName()).matches();
        }

        boolean propertyMatches(DTClass dtClass, FieldPath fp) {
            if (propertyPattern == null) return true;
            var fpMap = propertyMatches.get(dtClass);
            if (fpMap == null) {
                fpMap = new HashMap<>();
                propertyMatches.put(dtClass, fpMap);
            }
            var hit = fpMap.get(fp);
            if (hit == null) {
                hit = propertyPattern.matcher(dtClass.getNameForFieldPath(fp)).matches();
                fpMap.put(fp, hit);
            }
            return hit;
        }
    }

    @Initializer(OnEntityPropertyChanged.class)
    public void initListener(final EventListener<OnEntityPropertyChanged> listener) {
        adapters.add(new ListenerAdapter(listener));
        adapters.sort(Comparator.comparingInt(a -> a.listener.getOrder()));
    }

    private static final ListenerAdapter[] EMPTY = new ListenerAdapter[0];

    private ListenerAdapter[] adaptersFor(DTClass dtClass) {
        var arr = adaptersByClass.get(dtClass);
        if (arr == null) {
            var matching = new ArrayList<ListenerAdapter>(adapters.size());
            for (var a : adapters) {
                if (a.classMatches(dtClass)) matching.add(a);
            }
            arr = matching.isEmpty() ? EMPTY : matching.toArray(new ListenerAdapter[0]);
            adaptersByClass.put(dtClass, arr);
        }
        return arr;
    }

    private void dispatch(Entity e, FieldPath fp, ListenerAdapter[] interested) {
        var dtClass = e.getDtClass();
        for (var a : interested) {
            if (!a.propertyMatches(dtClass, fp)) continue;
            try {
                a.listener.invoke(e, fp);
            } catch (Throwable t) {
                evPropertyChanged.handleListenerException(new Object[]{e, fp}, t);
            }
        }
    }

    @OnEntityCreated
    @Order(1000)
    public void onEntityCreated(Entity e) {
        if (adapters.isEmpty()) return;
        var interested = adaptersFor(e.getDtClass());
        if (interested.length == 0) return;
        final var iter = e.getState().fieldPathIterator();
        while (iter.hasNext()) {
            dispatch(e, iter.next(), interested);
        }
    }

    @OnEntityUpdated
    @Order(1000)
    public void onUpdate(Entity e, FieldPath[] fieldPaths, int num) {
        if (adapters.isEmpty()) return;
        var interested = adaptersFor(e.getDtClass());
        if (interested.length == 0) return;
        for (var i = 0; i < num; i++) {
            dispatch(e, fieldPaths[i], interested);
        }
    }

}
