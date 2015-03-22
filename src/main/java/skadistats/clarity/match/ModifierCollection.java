package skadistats.clarity.match;

import com.rits.cloning.Cloner;
import skadistats.clarity.model.ModifierTableEntry;
import skadistats.clarity.wire.proto.DotaModifiers.CDOTAModifierBuffTableEntry;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ModifierCollection implements Cloneable {
    
    private static final Cloner CLONER = new Cloner();
    
    private final List<ModifierTableEntry> modifiers = new LinkedList<ModifierTableEntry>();
    private final Map<Integer, LinkedList<ModifierTableEntry>> modifiersForEntity = new HashMap<Integer, LinkedList<ModifierTableEntry>>();

    public ModifierCollection() {
    }
    
    public ModifierTableEntry add(CDOTAModifierBuffTableEntry tableEntry) {
    	ModifierTableEntry newModifier = new ModifierTableEntry(tableEntry);
        modifiers.add(newModifier);
        //int entityIndex = tableEntry.getParent() & 0x7FF;
        if(modifiersForEntity.containsKey(tableEntry.getParent())){
        	modifiersForEntity.get(tableEntry.getParent()).add(newModifier);
        }
        else{
        	LinkedList<ModifierTableEntry> list = new LinkedList<ModifierTableEntry>();
        	list.add(newModifier);
        	modifiersForEntity.put(tableEntry.getParent(), list);
        }
        return newModifier;
    }
    
    public void clear(){
    	modifiers.clear();
    	modifiersForEntity.clear();
    }
    
    public List<ModifierTableEntry> getAll() {
    	return modifiers;
    }

    public List<ModifierTableEntry> getAllForEntity(int handle) {
    	if(modifiersForEntity.containsKey(handle))
    		return modifiersForEntity.get(handle);
    	else
    		return new LinkedList<ModifierTableEntry>();
    }
    
    
    @Override
    public ModifierCollection clone() {
       return CLONER.deepClone(this);
    }
    

}
