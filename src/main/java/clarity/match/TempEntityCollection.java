package clarity.match;

import java.util.ArrayList;
import java.util.List;

import clarity.model.DTClass;
import clarity.model.Entity;

import com.rits.cloning.Cloner;

public class TempEntityCollection implements Cloneable {
    
    private static final Cloner CLONER = new Cloner();
    
    private final List<Entity> tempEntities = new ArrayList<Entity>();

    public void add(DTClass dtClass, Object[] state) {
        tempEntities.add(new Entity(0, 0, dtClass, null, state));
    }
    
    public void clear() {
        tempEntities.clear();
    }
    
    @Override
    public TempEntityCollection clone() {
       return CLONER.deepClone(this);
    }
    
}
