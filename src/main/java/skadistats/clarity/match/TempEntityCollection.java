package skadistats.clarity.match;

import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;

public class TempEntityCollection extends BaseCollection<TempEntityCollection, Entity> {
    
    public void add(DTClass dtClass, Object[] state) {
        values.add(new Entity(0, 0, dtClass, null, state));
    }
    
}
