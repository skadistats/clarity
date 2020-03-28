package skadistats.clarity.model.state;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;

public class EntityRegistry {

    private final Int2ObjectOpenHashMap<Entity> entities = new Int2ObjectOpenHashMap<>();

    public Entity create(int index, int serial, int handle, DTClass dtClass) {
        Entity entity = entities.get(handle);
        if (entity == null) {
            entity = new Entity(index, serial, handle, dtClass);
            entities.put(handle, entity);
        }
        entity.setState(null);
        entity.setExistent(false);
        entity.setActive(false);
        return entity;
    }

    public Entity get(int handle) {
        return entities.get(handle);
    }

}
