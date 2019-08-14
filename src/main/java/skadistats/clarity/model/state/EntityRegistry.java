package skadistats.clarity.model.state;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import skadistats.clarity.model.Entity;

import java.util.function.Supplier;

public class EntityRegistry {

    private final Int2ObjectOpenHashMap<Entity> entities = new Int2ObjectOpenHashMap<>();

    public Entity get(int handle, Supplier<Entity> constructor) {
        return entities.computeIfAbsent(handle,  h -> constructor.get());
    }

}
