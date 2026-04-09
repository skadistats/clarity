package skadistats.clarity.processor.entities;

import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Order;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;

@Provides({OnEntityPropertyChanged.class})
public class PropertyChange {

    @InsertEvent
    private OnEntityPropertyChanged.Event evPropertyChanged;

    @OnEntityCreated
    @Order(1000)
    public void onEntityCreated(Entity e) {
        if (!evPropertyChanged.isListenedTo()) return;
        final var iter = e.getState().fieldPathIterator();
        while (iter.hasNext()) {
            evPropertyChanged.raise(e, iter.next());
        }
    }

    @OnEntityUpdated
    @Order(1000)
    public void onUpdate(Entity e, FieldPath[] fieldPaths, int num) {
        if (!evPropertyChanged.isListenedTo()) return;
        for (var i = 0; i < num; i++) {
            evPropertyChanged.raise(e, fieldPaths[i]);
        }
    }

}
