package clarity.match;

import clarity.model.Entity;

public class EntityCollection {

    private final Entity[] entities = new Entity[2048];

    public void put(int index, Entity entity) {
        entities[index] = entity;
    }

    public Entity get(int index) {
        return entities[index];
    }

}
