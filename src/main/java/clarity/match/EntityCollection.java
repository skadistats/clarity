package clarity.match;

import clarity.model.DTClass;
import clarity.model.Entity;

public class EntityCollection {

    private final Entity[] entities = new Entity[0x800];

    public void add(int index, int serial, DTClass dtClass, Object[] state) {
        entities[index] = new Entity(index, serial, dtClass, state);
    }

    public Entity getByIndex(int index) {
        return entities[index];
    }
    
    public Entity getByEHandle(int eHandle) {
        Entity e = entities[eHandle & 0x7FF];
        return e == null || e.getSerial() != eHandle >> 11 ? null : e;
    }
    
    public void remove(int index) {
        entities[index] = null;
    }

}
