package skadistats.clarity.model;

import java.util.HashMap;
import java.util.Map;


public class Descriptor {

    private final String name;
    private final String[] keys;
    private Map<String, Integer> indexByKey = new HashMap<String, Integer>();
    
    
    public Descriptor(String name, String[] keys) {
        this.name = name;
        this.keys = keys;
        for (int i = 0; i < keys.length; i++) {
            indexByKey.put(keys[i], i);
        }
    }

    public String getName() {
        return name;
    }

    public String[] getKeys() {
        return keys;
    }
    
    public Integer getIndexForKey(String key) {
        return indexByKey.get(key);
    }
    
}
