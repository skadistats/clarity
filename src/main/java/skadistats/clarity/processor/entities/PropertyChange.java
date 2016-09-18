package skadistats.clarity.processor.entities;

import skadistats.clarity.event.Event;
import skadistats.clarity.event.EventListener;
import skadistats.clarity.event.Initializer;
import skadistats.clarity.event.Insert;
import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.EngineType;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;
import skadistats.clarity.util.Predicate;
import skadistats.clarity.util.TriState;
import skadistats.clarity.util.TriStateTable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Provides({OnEntityPropertyChanged.class})
public class PropertyChange {

    @Insert
    private EngineType engineType;
    @InsertEvent
    private Event<OnEntityPropertyChanged> evPropertyChanged;

    private final List<ListenerAdapter> adapters = new ArrayList<>();

    public class ListenerAdapter {

        private final Pattern classPattern;
        private final Pattern propertyPattern;
        private final TriStateTable classMatchesForEntity;
        private final Map<FieldPath, TriState>[] propertyMatchesForEntity;

        public ListenerAdapter(EventListener<OnEntityPropertyChanged> listener) {
            classPattern = Pattern.compile(listener.getAnnotation().classPattern());
            propertyPattern = Pattern.compile(listener.getAnnotation().propertyPattern());
            int count = 1 << engineType.getIndexBits();
            classMatchesForEntity = new TriStateTable(count);
            propertyMatchesForEntity = new Map[count];
            for (int i = 0; i < count; i++) {
                propertyMatchesForEntity[i] = new HashMap<>();
            }
        }

        private Predicate<Object[]> invocationPredicate = new Predicate<Object[]>() {
            @Override
            public boolean apply(Object[] value) {
                Entity e = (Entity) value[0];
                int eIdx = e.getIndex();
                TriState classMatchState = classMatchesForEntity.get(eIdx);
                if (classMatchState == TriState.UNSET) {
                    classMatchState = TriState.fromBoolean(classPattern.matcher(e.getDtClass().getDtName()).matches());
                    classMatchesForEntity.set(eIdx, classMatchState);
                }
                if (classMatchState != TriState.YES) {
                    return false;
                }
                FieldPath fp = (FieldPath) value[1];
                TriState propertyMatchState = propertyMatchesForEntity[eIdx].get(fp);
                if (propertyMatchState == null) {
                    propertyMatchState = TriState.fromBoolean(propertyPattern.matcher(e.getDtClass().getNameForFieldPath(fp)).matches());
                    propertyMatchesForEntity[eIdx].put(fp, propertyMatchState);
                }
                if (propertyMatchState != TriState.YES) {
                    return false;
                }
                return true;
            }
        };

        private void clear(Entity e) {
            int eIdx = e.getIndex();
            classMatchesForEntity.set(eIdx, TriState.UNSET);
            propertyMatchesForEntity[eIdx].clear();
        }
    }

    @Initializer(OnEntityPropertyChanged.class)
    public void initListener(final EventListener<OnEntityPropertyChanged> listener) {
        ListenerAdapter adapter = new ListenerAdapter(listener);
        adapters.add(adapter);
        listener.setInvocationPredicate(adapter.invocationPredicate);
    }

    @OnEntityCreated
    public void onEntityCreated(Entity e) {
        List<FieldPath> fieldPaths = e.getDtClass().collectFieldPaths(e.getState());
        for (FieldPath fp : fieldPaths) {
            evPropertyChanged.raise(e, fp);
        }
    }

    @OnEntityUpdated
    public void onUpdate(Entity e, FieldPath[] fieldPaths, int num) {
        for (int i = 0; i < num; i++) {
            evPropertyChanged.raise(e, fieldPaths[i]);
        }
    }

    @OnEntityDeleted
    public void onDeleted(Entity e) {
        for (ListenerAdapter adapter : adapters) {
            adapter.clear(e);
        }
    }

}
