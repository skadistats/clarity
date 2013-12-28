package clarity.match;

import java.util.ArrayList;
import java.util.List;

import com.dota2.proto.DotaModifiers.CDOTAModifierBuffTableEntry;

public class ModifierCollection {
    
    private final List<CDOTAModifierBuffTableEntry> modifiers = new ArrayList<CDOTAModifierBuffTableEntry>();

    public void set(int index, CDOTAModifierBuffTableEntry modifier) {
        if (index == modifiers.size()) {
            modifiers.add(modifier);
        } else if (index < modifiers.size()) {
            modifiers.set(index, modifier);
        } else {
            throw new RuntimeException("index too high for modifier");
        }
    }
    
    
}
