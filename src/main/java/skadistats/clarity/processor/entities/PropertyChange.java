package skadistats.clarity.processor.entities;

import skadistats.clarity.event.InsertEvent;
import skadistats.clarity.event.Order;
import skadistats.clarity.event.Provides;
import skadistats.clarity.model.Entity;
import skadistats.clarity.model.FieldPath;

/**
 * Built-in processor that raises {@link OnEntityPropertyChanged}: once per property when an entity
 * is created, and once per field path of each {@link OnEntityUpdated}. Used by the runtime.
 */
@Provides({OnEntityPropertyChanged.class})
public class PropertyChange {

    @InsertEvent
    private OnEntityPropertyChanged.Event evPropertyChanged;

    /** Raises {@link OnEntityPropertyChanged} for every property of the new entity. */
    @OnEntityCreated
    @Order(1000)
    public void onEntityCreated(Entity e) {
        if (!evPropertyChanged.isListenedTo()) return;
        final var iter = e.getState().fieldPathIterator();
        while (iter.hasNext()) {
            evPropertyChanged.raise(e, iter.next());
        }
    }

    /** Raises {@link OnEntityPropertyChanged} for the first {@code num} field paths. */
    @OnEntityUpdated
    @Order(1000)
    public void onUpdate(Entity e, FieldPath[] fieldPaths, int num) {
        if (!evPropertyChanged.isListenedTo()) return;
        for (var i = 0; i < num; i++) {
            evPropertyChanged.raise(e, fieldPaths[i]);
        }
    }

}
