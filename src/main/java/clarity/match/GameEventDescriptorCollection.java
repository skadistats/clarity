package clarity.match;

import java.util.Map;
import java.util.TreeMap;

import clarity.model.GameEventDescriptor;

public class GameEventDescriptorCollection {

    private final Map<Integer, GameEventDescriptor> byId = new TreeMap<Integer, GameEventDescriptor>();
    private final Map<String, GameEventDescriptor> byName = new TreeMap<String, GameEventDescriptor>();

    public void add(GameEventDescriptor descriptor) {
        byId.put(descriptor.getId(), descriptor);
        byName.put(descriptor.getName(), descriptor);
    }

    public GameEventDescriptor forName(String name) {
        return byName.get(name);
    }

    public GameEventDescriptor forId(int id) {
        return byId.get(id);
    }
    
    
}
