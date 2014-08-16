package skadistats.clarity.match;

import java.util.ArrayList;
import java.util.List;

import skadistats.clarity.model.DTClass;
import skadistats.clarity.model.Entity;

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
    
    public List<Entity> getAll(){
    	return tempEntities;
    }
    
    @Override
    public TempEntityCollection clone() {
       return CLONER.deepClone(this);
    }
    
}
