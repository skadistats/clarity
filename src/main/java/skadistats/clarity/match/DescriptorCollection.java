package skadistats.clarity.match;

import java.util.Map;
import java.util.TreeMap;

import skadistats.clarity.model.Descriptor;

public class DescriptorCollection {

    private final Map<String, Descriptor> byName = new TreeMap<String, Descriptor>();

    public void add(Descriptor descriptor) {
        byName.put(descriptor.getName(), descriptor);
    }

    public Descriptor forName(String name) {
        return byName.get(name);
    }    
    
}
