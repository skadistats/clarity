package skadistats.clarity.match;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.dota2.proto.DotaModifiers.CDOTAModifierBuffTableEntry;
import com.rits.cloning.Cloner;

public class ModifierCollection implements Cloneable {
    
    private static final Cloner CLONER = new Cloner();
    
    private final List<Map<Integer, CDOTAModifierBuffTableEntry>> modifiers = new ArrayList<Map<Integer, CDOTAModifierBuffTableEntry>>(2048);

    public ModifierCollection() {
        for (int i = 0; i < 2048; i++) {
            modifiers.add(null);
        }
    }
    
    public void set(int entityIndex, int modifierIndex, CDOTAModifierBuffTableEntry modifier) {
        Map<Integer, CDOTAModifierBuffTableEntry> modsForEntity = modifiers.get(entityIndex);
        if (modsForEntity == null) {
            modsForEntity = new TreeMap<Integer, CDOTAModifierBuffTableEntry>();
            modifiers.set(entityIndex, modsForEntity);
        }
        modsForEntity.put(modifierIndex, modifier);
    }
    
    public void remove(int entityIndex, int modifierIndex) {
        modifiers.get(entityIndex).remove(modifierIndex);
    }
    
    public CDOTAModifierBuffTableEntry get(int entityIndex, int modifierIndex) {
        try {
            return modifiers.get(entityIndex).get(modifierIndex);
        } catch (NullPointerException e) {
            return null;
        }
    }
    
    @Override
    public ModifierCollection clone() {
       return CLONER.deepClone(this);
    }
    

}
