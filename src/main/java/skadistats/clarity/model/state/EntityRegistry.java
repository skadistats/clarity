package skadistats.clarity.model.state;

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;

public class EntityRegistry {

    private final Long2ObjectOpenHashMap<Entity> entities = new Long2ObjectOpenHashMap<>();

    public Entity create(int dtClassId, int index, int serial, int handle, DTClass dtClass) {
        long uid = Entity.uid(dtClassId, handle);
        Entity entity = entities.get(uid);
        if (entity == null) {
            entity = new Entity(index, serial, handle, dtClass);
            entities.put(uid, entity);
        }
        entity.setState(null);
        entity.setExistent(false);
        entity.setActive(false);
        return entity;
    }

    public Entity get(long uid) {
        return entities.get(uid);
    }

}
